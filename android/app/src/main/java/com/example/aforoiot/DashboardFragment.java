package com.example.aforoiot;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private static final long INTERVALO_REFRESCO_MS = 5000L;

    private Handler manejador;
    private Runnable refresco;
    private SessionManager sesion;

    private Chip chipEstado;
    private TextView txtDentro;
    private TextView txtAforoMaximo;
    private TextView txtDetalleAforo;
    private TextView txtTemperatura;
    private TextView txtHumedad;
    private TextView txtActualizado;
    private LinearProgressIndicator progresoAforo;

    private boolean cargando;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        sesion = new SessionManager(requireContext());
        chipEstado = vista.findViewById(R.id.chipEstado);
        txtDentro = vista.findViewById(R.id.txtDentro);
        txtAforoMaximo = vista.findViewById(R.id.txtAforoMaximo);
        txtDetalleAforo = vista.findViewById(R.id.txtDetalleAforo);
        txtTemperatura = vista.findViewById(R.id.txtTemperatura);
        txtHumedad = vista.findViewById(R.id.txtHumedad);
        txtActualizado = vista.findViewById(R.id.txtActualizado);
        progresoAforo = vista.findViewById(R.id.progresoAforo);
    }

    @Override
    public void onResume() {
        super.onResume();
        iniciarRefresco();
    }

    @Override
    public void onPause() {
        detenerRefresco();
        super.onPause();
    }

    private void iniciarRefresco() {
        if (manejador == null) {
            manejador = new Handler(Looper.getMainLooper());
        }
        refresco = new Runnable() {
            @Override
            public void run() {
                cargarEstado();
                manejador.postDelayed(this, INTERVALO_REFRESCO_MS);
            }
        };
        manejador.post(refresco);
    }

    private void detenerRefresco() {
        if (manejador != null && refresco != null) {
            manejador.removeCallbacks(refresco);
        }
    }

    private void cargarEstado() {
        if (!isAdded() || cargando) {
            return;
        }
        cargando = true;
        Api.get(requireContext(), "app/estado.php", this::actualizarEstado, this::manejarError);
    }

    private void actualizarEstado(JSONObject respuesta) {
        cargando = false;
        if (!isAdded()) {
            return;
        }
        if (!respuesta.optBoolean("success", false)) {
            mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
            return;
        }

        int dentro = respuesta.optInt("dentro", 0);
        int aforoMax = respuesta.isNull("aforo_max") ? 0 : respuesta.optInt("aforo_max", 0);
        boolean enLinea = respuesta.optBoolean("en_linea", false);

        actualizarChip(enLinea);
        txtDentro.setText(String.valueOf(dentro));
        txtAforoMaximo.setText(aforoMax > 0 ? getString(R.string.aforo_maximo_valor, aforoMax) : "");

        if (aforoMax > 0) {
            int ocupacion = Math.min(dentro, aforoMax);
            progresoAforo.setVisibility(View.VISIBLE);
            progresoAforo.setMax(aforoMax);
            progresoAforo.setProgressCompat(ocupacion, true);
            progresoAforo.setIndicatorColor(colorIndicador(dentro, aforoMax));
            txtDetalleAforo.setText(getString(R.string.detalle_aforo, dentro, aforoMax));
        } else {
            progresoAforo.setVisibility(View.GONE);
            txtDetalleAforo.setText(R.string.aforo_no_configurado);
        }

        JSONObject medicion = respuesta.optJSONObject("medicion");
        if (medicion != null) {
            txtTemperatura.setText(getString(R.string.temperatura_formato,
                    medicion.optDouble("temperatura", 0)));
            txtHumedad.setText(getString(R.string.humedad_formato,
                    medicion.optDouble("humedad", 0)));
        } else {
            txtTemperatura.setText(R.string.temperatura_vacia);
            txtHumedad.setText(R.string.humedad_vacia);
        }

        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        txtActualizado.setText(getString(R.string.actualizado, hora));
    }

    private void actualizarChip(boolean enLinea) {
        chipEstado.setText(enLinea ? R.string.esp32_en_linea : R.string.esp32_sin_conexion);
        int fondo = enLinea ? R.color.statusOnlineContainer : R.color.errorContainer;
        int contenido = enLinea ? R.color.statusOnline : R.color.onErrorContainer;
        chipEstado.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), fondo)));
        chipEstado.setTextColor(ContextCompat.getColor(requireContext(), contenido));
    }

    private int colorIndicador(int dentro, int aforoMax) {
        int porcentaje = (int) (dentro * 100L / aforoMax);
        int recurso;
        if (porcentaje < 80) {
            recurso = R.color.primary;
        } else if (porcentaje < 100) {
            recurso = R.color.statusWarning;
        } else {
            recurso = R.color.error;
        }
        return ContextCompat.getColor(requireContext(), recurso);
    }

    private void manejarError(VolleyError error) {
        cargando = false;
        if (!isAdded()) {
            return;
        }
        if (Api.esNoAutorizado(error)) {
            sesion.cerrarSesion();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return;
        }
        mostrarMensaje(Api.mensajeDeError(requireContext(), error));
    }

    private void mostrarMensaje(String mensaje) {
        View vista = getView();
        if (vista != null) {
            Snackbar.make(vista, mensaje, Snackbar.LENGTH_LONG).show();
        }
    }
}
