package com.example.aforoiot;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DispositivosActivity extends AppCompatActivity implements DispositivosAdapter.Acciones {
    private DispositivosAdapter adaptador;
    private RecyclerView rvDispositivos;
    private View layoutVacio;
    private SessionManager sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SessionManager(this);
        if (!sesion.estaLogueado()) {
            abrirLogin();
            return;
        }

        setContentView(R.layout.activity_dispositivos);
        MaterialToolbar toolbar = findViewById(R.id.toolbarDispositivos);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvDispositivos = findViewById(R.id.rvDispositivos);
        layoutVacio = findViewById(R.id.layoutVacioDispositivos);

        adaptador = new DispositivosAdapter(this);
        rvDispositivos.setLayoutManager(new LinearLayoutManager(this));
        rvDispositivos.setAdapter(adaptador);

        FloatingActionButton fab = findViewById(R.id.fabAgregarDispositivo);
        fab.setOnClickListener(v -> abrirFormulario(null));

        cargarDispositivos();
    }

    private void cargarDispositivos() {
        Api.get(this, "app/dispositivos.php", this::actualizarDispositivos,
                error -> ManejoErrores.manejar(this, findViewById(android.R.id.content),
                        error, this::cargarDispositivos, true));
    }

    private void actualizarDispositivos(JSONObject respuesta) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (!respuesta.optBoolean("success", false)) {
            mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
            return;
        }

        List<Dispositivo> dispositivos = new ArrayList<>();
        JSONArray arreglo = respuesta.optJSONArray("dispositivos");
        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject item = arreglo.optJSONObject(i);
                if (item != null) {
                    dispositivos.add(new Dispositivo(
                            item.optInt("id"),
                            item.optString("nombre"),
                            item.isNull("ubicacion") ? null : item.optString("ubicacion"),
                            item.optString("api_key"),
                            item.optBoolean("activo", false),
                            item.optBoolean("en_linea", false)));
                }
            }
        }

        adaptador.actualizar(dispositivos);
        boolean vacio = dispositivos.isEmpty();
        layoutVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvDispositivos.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void abrirFormulario(Dispositivo dispositivo) {
        boolean esEdicion = dispositivo != null;
        View formulario = getLayoutInflater().inflate(R.layout.dialog_dispositivo, null);
        TextInputLayout tilNombre = formulario.findViewById(R.id.tilNombreDispositivo);
        TextInputLayout tilUbicacion = formulario.findViewById(R.id.tilUbicacionDispositivo);
        TextInputEditText etNombre = formulario.findViewById(R.id.etNombreDispositivo);
        TextInputEditText etUbicacion = formulario.findViewById(R.id.etUbicacionDispositivo);

        if (esEdicion) {
            etNombre.setText(dispositivo.getNombre());
            etUbicacion.setText(dispositivo.getUbicacion());
        }

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(esEdicion ? R.string.editar_dispositivo : R.string.agregar_dispositivo)
                .setView(formulario)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.guardar, null)
                .create();
        dialogo.show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            tilNombre.setError(null);
            String nombre = texto(etNombre);
            String ubicacion = texto(etUbicacion);
            if (nombre.isEmpty()) {
                tilNombre.setError(getString(R.string.campo_requerido));
                return;
            }

            JSONObject cuerpo = new JSONObject();
            try {
                cuerpo.put("accion", esEdicion ? "editar" : "crear");
                if (esEdicion) {
                    cuerpo.put("id", dispositivo.getId());
                }
                cuerpo.put("nombre", nombre);
                cuerpo.put("ubicacion", ubicacion);
            } catch (JSONException e) {
                mostrarMensaje(getString(R.string.error_servidor));
                return;
            }

            dialogo.dismiss();
            Api.post(this, "app/dispositivos.php", cuerpo,
                    respuesta -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (!respuesta.optBoolean("success", false)) {
                            mostrarMensaje(respuesta.optString("mensaje",
                                    getString(R.string.error_servidor)));
                            return;
                        }
                        mostrarMensaje(getString(R.string.dispositivo_guardado));
                        cargarDispositivos();
                    },
                    error -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        ManejoErrores.manejar(this, findViewById(android.R.id.content),
                                error, null, true);
                        cargarDispositivos();
                    });
        });
    }

    private void enviarAccion(String accion, Dispositivo dispositivo) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("accion", accion);
            cuerpo.put("id", dispositivo.getId());
        } catch (JSONException e) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        Api.post(this, "app/dispositivos.php", cuerpo,
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje",
                                getString(R.string.error_servidor)));
                        cargarDispositivos();
                        return;
                    }
                    cargarDispositivos();
                },
                error -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ManejoErrores.manejar(this, findViewById(android.R.id.content),
                            error, null, true);
                    cargarDispositivos();
                });
    }

    @Override
    public void copiar(Dispositivo dispositivo) {
        ClipboardManager portapapeles =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        portapapeles.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.api_key), dispositivo.getApiKey()));
        mostrarMensaje(getString(R.string.api_key_copiada));
    }

    @Override
    public void editar(Dispositivo dispositivo) {
        abrirFormulario(dispositivo);
    }

    @Override
    public void cambiarActivo(Dispositivo dispositivo, boolean activo) {
        enviarAccion(activo ? "activar" : "desactivar", dispositivo);
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

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
