package com.example.aforoiot;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class AlertService extends Service {
    private static final String CANAL_SERVICIO = "servicio_alertas";
    private static final String CANAL_ALERTAS = "alertas_aforo";
    private static final String PREFERENCIAS = "alertas_aforo";
    private static final String ULTIMA_ALERTA = "ultima_alerta_id";
    private static final long INTERVALO_MS = 10000L;

    private Handler manejador;
    private Runnable consulta;
    private boolean inicializado;

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanales();
        startForeground(1, notificacionServicio());

        manejador = new Handler(Looper.getMainLooper());
        consulta = new Runnable() {
            @Override
            public void run() {
                revisarAlertas();
                manejador.postDelayed(this, INTERVALO_MS);
            }
        };
        manejador.post(consulta);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, notificacionServicio());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (manejador != null && consulta != null) {
            manejador.removeCallbacks(consulta);
        }
        super.onDestroy();
    }

    private void crearCanales() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager gestor = getSystemService(NotificationManager.class);
        if (gestor == null) {
            return;
        }
        gestor.createNotificationChannel(new NotificationChannel(
                CANAL_SERVICIO,
                getString(R.string.canal_servicio),
                NotificationManager.IMPORTANCE_LOW));
        gestor.createNotificationChannel(new NotificationChannel(
                CANAL_ALERTAS,
                getString(R.string.canal_alertas),
                NotificationManager.IMPORTANCE_HIGH));
    }

    private Notification notificacionServicio() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendiente = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CANAL_SERVICIO)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(getString(R.string.servicio_alertas))
                .setContentText(getString(R.string.servicio_alertas_texto))
                .setOngoing(true)
                .setContentIntent(pendiente)
                .build();
    }

    private void revisarAlertas() {
        Api.get(this, "app/alertas.php?atendida=0&limite=1",
                respuesta -> {
                    if (!respuesta.optBoolean("success", false)) {
                        return;
                    }
                    JSONArray alertas = respuesta.optJSONArray("alertas");
                    JSONObject alerta = alertas != null && alertas.length() > 0
                            ? alertas.optJSONObject(0)
                            : null;

                    SharedPreferences preferencias = getSharedPreferences(
                            PREFERENCIAS, Context.MODE_PRIVATE);
                    int ultima = preferencias.getInt(ULTIMA_ALERTA, 0);

                    if (!inicializado) {
                        inicializado = true;
                        preferencias.edit()
                                .putInt(ULTIMA_ALERTA, alerta != null ? alerta.optInt("id", 0) : 0)
                                .apply();
                        return;
                    }
                    if (alerta == null) {
                        return;
                    }

                    int id = alerta.optInt("id", 0);
                    if (id < ultima) {
                        preferencias.edit().putInt(ULTIMA_ALERTA, id).apply();
                        return;
                    }
                    if (id == ultima) {
                        return;
                    }
                    preferencias.edit().putInt(ULTIMA_ALERTA, id).apply();
                    notificar(alerta);
                },
                error -> {
                    if (Api.esNoAutorizado(error)) {
                        stopSelf();
                    }
                });
    }

    private void notificar(JSONObject alerta) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendiente = PendingIntent.getActivity(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder constructor = new NotificationCompat.Builder(
                this, CANAL_ALERTAS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(getString(R.string.alerta_notificacion_titulo))
                .setContentText(alerta.optString("mensaje", ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendiente);

        try {
            NotificationManagerCompat.from(this)
                    .notify(1000 + alerta.optInt("id", 0), constructor.build());
        } catch (SecurityException ignorada) {
            // Sin permiso de notificaciones: se ignora.
        }
    }
}
