package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistroActivity extends AppCompatActivity {
    public static final String EXTRA_CORREO = "correo";

    private TextInputLayout tilNombre;
    private TextInputLayout tilCorreo;
    private TextInputLayout tilContrasena;
    private TextInputEditText etNombre;
    private TextInputEditText etCorreo;
    private TextInputEditText etContrasena;
    private MaterialButton btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        tilNombre = findViewById(R.id.tilNombre);
        tilCorreo = findViewById(R.id.tilCorreo);
        tilContrasena = findViewById(R.id.tilContrasena);
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> registrar());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    private void registrar() {
        limpiarErrores();

        String nombre = texto(etNombre);
        String correo = texto(etCorreo);
        String contrasena = texto(etContrasena);

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.campo_requerido));
            etNombre.requestFocus();
            return;
        }
        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.campo_requerido));
            etCorreo.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.setError(getString(R.string.correo_invalido));
            etCorreo.requestFocus();
            return;
        }
        if (contrasena.length() < 6) {
            tilContrasena.setError(getString(R.string.contrasena_corta));
            etContrasena.requestFocus();
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("nombre", nombre);
            cuerpo.put("correo", correo);
            cuerpo.put("password", contrasena);
        } catch (JSONException e) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        btnRegistrar.setEnabled(false);
        Api.post(this, "auth/registro.php", cuerpo,
                respuesta -> {
                    btnRegistrar.setEnabled(true);
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    Intent resultado = new Intent();
                    resultado.putExtra(EXTRA_CORREO, correo);
                    setResult(RESULT_OK, resultado);
                    finish();
                },
                error -> {
                    btnRegistrar.setEnabled(true);
                    mostrarMensaje(Api.mensajeDeError(this, error));
                });
    }

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilCorreo.setError(null);
        tilContrasena.setError(null);
    }

    private void mostrarMensaje(String mensaje) {
        Snackbar.make(btnRegistrar, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
