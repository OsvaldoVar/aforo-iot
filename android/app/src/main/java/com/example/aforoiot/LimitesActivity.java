package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class LimitesActivity extends AppCompatActivity {
    private TextInputLayout tilTempMax;
    private TextInputLayout tilHumMax;
    private TextInputLayout tilAforoMax;
    private TextInputEditText etTempMax;
    private TextInputEditText etHumMax;
    private TextInputEditText etAforoMax;
    private MaterialButton btnGuardar;
    private android.widget.TextView txtDispositivo;
    private SessionManager sesion;
    private int dispositivoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SessionManager(this);
        if (!sesion.estaLogueado()) {
            abrirLogin();
            return;
        }

        setContentView(R.layout.activity_limites);
        MaterialToolbar toolbar = findViewById(R.id.toolbarLimites);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTempMax = findViewById(R.id.tilTempMax);
        tilHumMax = findViewById(R.id.tilHumMax);
        tilAforoMax = findViewById(R.id.tilAforoMax);
        etTempMax = findViewById(R.id.etTempMax);
        etHumMax = findViewById(R.id.etHumMax);
        etAforoMax = findViewById(R.id.etAforoMax);
        btnGuardar = findViewById(R.id.btnGuardarLimites);
        txtDispositivo = findViewById(R.id.txtDispositivoLimites);

        btnGuardar.setOnClickListener(v -> guardar());
        cargarDispositivo();
    }

    private void cargarDispositivo() {
        Api.get(this, "app/estado.php",
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    dispositivoId = respuesta.optInt("dispositivo_id", -1);
                    String nombre = respuesta.optString("nombre_dispositivo", "");
                    txtDispositivo.setText(getString(R.string.dispositivo_limites,
                            nombre, dispositivoId));
                    cargarLimites();
                },
                error -> ManejoErrores.manejar(this, findViewById(android.R.id.content),
                        error, this::cargarDispositivo, true));
    }

    private void cargarLimites() {
        if (dispositivoId <= 0) {
            mostrarMensaje(getString(R.string.dispositivo_no_disponible));
            return;
        }
        Api.get(this, "app/limites.php?dispositivo_id=" + dispositivoId,
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    JSONObject limites = respuesta.optJSONObject("limites");
                    if (limites != null) {
                        etTempMax.setText(String.format(Locale.US, "%.1f",
                                limites.optDouble("temp_max", 0)));
                        etHumMax.setText(String.format(Locale.US, "%.1f",
                                limites.optDouble("hum_max", 0)));
                        etAforoMax.setText(String.valueOf(limites.optInt("aforo_max", 0)));
                    }
                },
                error -> ManejoErrores.manejar(this, findViewById(android.R.id.content),
                        error, this::cargarLimites, true));
    }

    private void guardar() {
        limpiarErrores();

        Double temp = numero(etTempMax);
        Double hum = numero(etHumMax);
        Integer aforo = entero(etAforoMax);

        if (temp == null || temp < 0 || temp > 60) {
            tilTempMax.setError(getString(R.string.rango_temp));
            return;
        }
        if (hum == null || hum < 0 || hum > 100) {
            tilHumMax.setError(getString(R.string.rango_hum));
            return;
        }
        if (aforo == null || aforo <= 0) {
            tilAforoMax.setError(getString(R.string.aforo_invalido));
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("dispositivo_id", dispositivoId);
            cuerpo.put("temp_max", temp);
            cuerpo.put("hum_max", hum);
            cuerpo.put("aforo_max", aforo);
        } catch (JSONException e) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        btnGuardar.setEnabled(false);
        Api.post(this, "app/limites.php", cuerpo,
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnGuardar.setEnabled(true);
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    mostrarMensaje(getString(R.string.limites_guardados));
                },
                error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnGuardar.setEnabled(true);
                    ManejoErrores.manejar(this, findViewById(android.R.id.content),
                            error, null, true);
                });
    }

    private void limpiarErrores() {
        tilTempMax.setError(null);
        tilHumMax.setError(null);
        tilAforoMax.setError(null);
    }

    private Double numero(TextInputEditText campo) {
        try {
            String valor = campo.getText() == null ? "" : campo.getText().toString().trim();
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer entero(TextInputEditText campo) {
        try {
            String valor = campo.getText() == null ? "" : campo.getText().toString().trim();
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void abrirLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarMensaje(String mensaje) {
        Snackbar.make(findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show();
    }
}
