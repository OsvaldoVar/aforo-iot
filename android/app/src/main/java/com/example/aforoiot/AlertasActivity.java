package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AlertasActivity extends AppCompatActivity implements AlertasAdapter.Acciones {
    private AlertasAdapter adaptador;
    private RecyclerView rvAlertas;
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

        setContentView(R.layout.activity_alertas);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAlertas);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAlertas = findViewById(R.id.rvAlertas);
        layoutVacio = findViewById(R.id.layoutVacioAlertas);

        adaptador = new AlertasAdapter(this);
        rvAlertas.setLayoutManager(new LinearLayoutManager(this));
        rvAlertas.setAdapter(adaptador);

        cargarAlertas();
    }

    private void cargarAlertas() {
        Api.get(this, "app/alertas.php?limite=100",
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    List<Alerta> alertas = new ArrayList<>();
                    JSONArray arreglo = respuesta.optJSONArray("alertas");
                    if (arreglo != null) {
                        for (int i = 0; i < arreglo.length(); i++) {
                            JSONObject item = arreglo.optJSONObject(i);
                            if (item != null) {
                                alertas.add(new Alerta(
                                        item.optInt("id"),
                                        item.optString("tipo"),
                                        item.optString("mensaje"),
                                        item.optString("fecha"),
                                        item.optBoolean("atendida", false)));
                            }
                        }
                    }
                    adaptador.actualizar(alertas);
                    boolean vacio = alertas.isEmpty();
                    layoutVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
                    rvAlertas.setVisibility(vacio ? View.GONE : View.VISIBLE);
                },
                error -> ManejoErrores.manejar(this, findViewById(android.R.id.content),
                        error, this::cargarAlertas, true));
    }

    @Override
    public void atender(Alerta alerta) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("id", alerta.getId());
        } catch (JSONException e) {
            mostrarMensaje(getString(R.string.error_servidor));
            return;
        }

        Api.post(this, "app/alertas.php", cuerpo,
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                        return;
                    }
                    mostrarMensaje(getString(R.string.alerta_atendida));
                    cargarAlertas();
                },
                error -> ManejoErrores.manejar(this, findViewById(android.R.id.content),
                        error, null, true));
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
