package com.example.aforoiot;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AlertaMonitor {
    private static final String CANAL = "alertas_aforo";
    private static final String PREFERENCIAS = "alertas_aforo";
    private static final String ULTIMA_ALERTA = "ultima_alerta_id";
    private static final long INTERVALO_MS = 10000L;

    private static Handler manejador;
    private static Runnable consulta;
    private static Context aplicacion;
    private static boolean inicializado;

    private AlertaMonitor() {
    }

    public static synchronized void iniciar(Context contexto) {
        if (consulta != null) {
            return;
        }
        aplicacion = contexto.getApplicationContext();
        inicializado = false;
        crearCanal();
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

    public static synchronized void detener() {
        if (manejador != null && consulta != null) {
            manejador.removeCallbacks(consulta);
        }
        manejador = null;
        consulta = null;
        inicializado = false;
    }

    private static void crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager gestor = aplicacion.getSystemService(NotificationManager.class);
        if (gestor == null) {
            return;
        }
        NotificationChannel canal = new NotificationChannel(
                CANAL,
                aplicacion.getString(R.string.canal_alertas),
                NotificationManager.IMPORTANCE_HIGH);
        gestor.createNotificationChannel(canal);
    }

    private static void revisarAlertas() {
        if (aplicacion == null) {
            return;
        }
        Api.get(aplicacion, "app/alertas.php?atendida=0&limite=1",
                respuesta -> {
                    if (!respuesta.optBoolean("success", false)) {
                        return;
                    }
                    JSONArray alertas = respuesta.optJSONArray("alertas");
                    if (alertas == null || alertas.length() == 0) {
                        return;
                    }
                    JSONObject alerta = alertas.optJSONObject(0);
                    if (alerta == null) {
                        return;
                    }

                    int id = alerta.optInt("id", 0);
                    SharedPreferences preferencias = aplicacion.getSharedPreferences(
                            PREFERENCIAS, Context.MODE_PRIVATE);
                    int ultima = preferencias.getInt(ULTIMA_ALERTA, 0);

                    if (!inicializado) {
                        preferencias.edit().putInt(ULTIMA_ALERTA, id).apply();
                        inicializado = true;
                        return;
                    }
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
                        detener();
                    }
                });
    }

    private static void notificar(JSONObject alerta) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(aplicacion,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(aplicacion, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendiente = PendingIntent.getActivity(
                aplicacion,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder constructor = new NotificationCompat.Builder(aplicacion, CANAL)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(aplicacion.getString(R.string.alerta_notificacion_titulo))
                .setContentText(alerta.optString("mensaje", ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendiente);

        try {
            NotificationManagerCompat.from(aplicacion)
                    .notify(alerta.optInt("id", 1), constructor.build());
        } catch (SecurityException ignorada) {
            // Sin permiso de notificaciones: se ignora.
        }
    }
}
