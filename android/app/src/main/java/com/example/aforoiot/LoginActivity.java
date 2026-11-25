package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilCorreo;
    private TextInputLayout tilContrasena;
    private TextInputEditText etCorreo;
    private TextInputEditText etContrasena;
    private MaterialButton btnLogin;
    private MaterialButton btnIrRegistro;
    private TextView txtServidor;
    private SessionManager sesion;

    private final ActivityResultLauncher<Intent> registroLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    etCorreo.setText(resultado.getData().getStringExtra(RegistroActivity.EXTRA_CORREO));
                    mostrarMensaje(getString(R.string.cuenta_creada));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SessionManager(this);
        if (sesion.estaLogueado()) {
            abrirPrincipal();
            return;
        }

        setContentView(R.layout.activity_login);
        tilCorreo = findViewById(R.id.tilCorreo);
        tilContrasena = findViewById(R.id.tilContrasena);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnLogin = findViewById(R.id.btnLogin);
        btnIrRegistro = findViewById(R.id.btnIrRegistro);
        txtServidor = findViewById(R.id.txtServidor);

        btnLogin.setOnClickListener(v -> iniciarSesion());
        btnIrRegistro.setOnClickListener(v ->
                registroLauncher.launch(new Intent(this, RegistroActivity.class)));

        txtServidor.setText(getString(R.string.servidor_actual, Config.baseUrl(this)));
        findViewById(R.id.btnServidor).setOnClickListener(v ->
                DialogoServidor.mostrar(this, () -> {
                    txtServidor.setText(getString(R.string.servidor_actual, Config.baseUrl(this)));
                    mostrarMensaje(getString(R.string.servidor_guardado));
                }));
    }

    private void iniciarSesion() {
        tilCorreo.setError(null);
        tilContrasena.setError(null);

        String correo = texto(etCorreo);
        String contrasena = texto(etContrasena);

        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.campo_requerido));
            etCorreo.requestFocus();
            return;
        }
        if (contrasena.isEmpty()) {
            tilContrasena.setError(getString(R.string.campo_requerido));
            etContrasena.requestFocus();
            return;
        }

        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("correo", correo);
            cuerpo.put("password", contrasena);
        } catch (JSONException e) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        bloquear(true);
        Api.post(this, "auth/login.php", cuerpo,
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    bloquear(false);
                    procesarLogin(respuesta);
                },
                error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    bloquear(false);
                    ManejoErrores.manejar(this, btnLogin, error, this::iniciarSesion, false);
                });
    }

    private void procesarLogin(JSONObject respuesta) {
        if (!respuesta.optBoolean("success", false)) {
            mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
            return;
        }

        String token = respuesta.optString("token", "");
        if (token.isEmpty()) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        sesion.guardarSesion(token, respuesta.optString("nombre", ""));
        abrirPrincipal();
    }

    private void abrirPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void bloquear(boolean bloqueado) {
        btnLogin.setEnabled(!bloqueado);
        btnIrRegistro.setEnabled(!bloqueado);
    }

    private void mostrarMensaje(String mensaje) {
        Snackbar.make(btnLogin, mensaje, Snackbar.LENGTH_LONG).show();
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
