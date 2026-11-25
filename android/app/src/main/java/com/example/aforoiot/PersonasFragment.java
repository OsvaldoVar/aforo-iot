package com.example.aforoiot;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PersonasFragment extends Fragment {
    private OperacionPersonas operacion;
    private PersonasAdapter adaptador;
    private JSONArray personas = new JSONArray();
    private Request<JSONObject> consulta;
    private AlertDialog dialogo;
    private boolean ocupada;
    private Snackbar avisoReintento;
    private Snackbar avisoOperacion;

    public PersonasFragment() {
        super(R.layout.fragment_personas);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);
        RecyclerView lista = vista.findViewById(R.id.rvPersonas);
        adaptador = new PersonasAdapter(this::asignarTarjeta);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        lista.setAdapter(adaptador);
        vista.findViewById(R.id.fabRegistrarPersona).setOnClickListener(v -> pedirNombre(null));
        vista.findViewById(R.id.btnReintentarPersonas).setOnClickListener(v -> cargarListas());
        operacion = new ViewModelProvider(requireActivity()).get(OperacionPersonas.class);
        operacion.resultado().observe(getViewLifecycleOwner(), this::mostrarResultado);
        // El GET debe comenzar después del POST que sobrevivió a la vista anterior.
        if (operacion.enCurso() || operacion.resultado().getValue() != null) {
            habilitarAcciones(false);
        } else {
            cargarListas();
        }
    }

    private void cargarListas() {
        if (operacion.enCurso()) {
            return;
        }
        View vista = requireView();
        if (avisoReintento != null) {
            avisoReintento.dismiss();
            avisoReintento = null;
        }
        if (consulta != null) {
            consulta.cancel();
        }
        habilitarAcciones(false);
        vista.findViewById(R.id.progresoPersonas).setVisibility(View.VISIBLE);
        vista.findViewById(R.id.btnReintentarPersonas).setVisibility(View.GONE);
        consulta = Api.getSinCache(requireContext(), OperacionPersonas.RUTA, respuesta -> {
            if (getView() != vista) {
                return;
            }
            try {
                if (!respuesta.optBoolean("success")) {
                    mostrarErrorCarga(respuesta.optString("mensaje", getString(R.string.error_servidor)));
                    return;
                }
                JSONArray recibidas = respuesta.getJSONArray("personas");
                JSONArray pendientes = respuesta.getJSONArray("pendientes");
                for (int indice = 0; indice < recibidas.length(); indice++) {
                    recibidas.getJSONObject(indice);
                }
                for (int indice = 0; indice < pendientes.length(); indice++) {
                    pendientes.getJSONObject(indice);
                }
                personas = recibidas;
                adaptador.actualizar(personas, pendientes);
                vista.findViewById(R.id.progresoPersonas).setVisibility(View.GONE);
                vista.findViewById(R.id.rvPersonas).setVisibility(View.VISIBLE);
                habilitarAcciones(true);
            } catch (JSONException error) {
                mostrarErrorCarga(getString(R.string.error_servidor));
            }
        }, error -> {
            if (getView() == vista && !sesionVencida(error)) {
                mostrarErrorCarga(Api.mensajeDeError(requireContext(), error));
            }
        });
    }

    private void mostrarErrorCarga(String mensaje) {
        View vista = requireView();
        vista.findViewById(R.id.progresoPersonas).setVisibility(View.GONE);
        vista.findViewById(R.id.btnReintentarPersonas).setVisibility(View.VISIBLE);
        if (avisoOperacion == null || !avisoOperacion.isShownOrQueued()) {
            avisoReintento = Snackbar.make(vista, mensaje, Snackbar.LENGTH_LONG)
                    .setAction(R.string.reintentar, v -> cargarListas());
            avisoReintento.show();
        }
    }

    private void habilitarAcciones(boolean habilitadas) {
        ocupada = !habilitadas;
        requireView().findViewById(R.id.fabRegistrarPersona).setEnabled(habilitadas);
        adaptador.habilitar(habilitadas);
    }

    private void pedirNombre(@Nullable String uid) {
        if (ocupada) {
            return;
        }
        View formulario = getLayoutInflater().inflate(R.layout.dialogo_nombre_persona, null);
        TextInputLayout campo = formulario.findViewById(R.id.tilNombrePersona);
        TextInputEditText entrada = formulario.findViewById(R.id.etNombrePersona);
        int titulo = uid == null ? R.string.persona_registrar : R.string.persona_nueva;
        dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titulo)
                .setView(formulario)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.persona_guardar, null)
                .show();
        AlertDialog formularioActivo = dialogo;
        formularioActivo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = entrada.getText() == null ? "" : entrada.getText().toString().trim();
            if (nombre.isEmpty()) {
                campo.setError(getString(R.string.campo_requerido));
                return;
            }
            if (nombre.codePointCount(0, nombre.length()) > 100) {
                campo.setError(getString(R.string.persona_nombre_largo));
                return;
            }
            formularioActivo.dismiss();
            guardarPersona(uid, nombre, 0);
        });
    }

    private void asignarTarjeta(String uid) {
        if (ocupada) {
            return;
        }
        String[] opciones = personas.length() == 0
                ? new String[] {getString(R.string.persona_nueva)}
                : new String[] {getString(R.string.persona_nueva), getString(R.string.persona_existente)};
        dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.tarjeta_asignar_descripcion, uid))
                .setItems(opciones, (ventana, opcion) -> {
                    if (opcion == 0) {
                        pedirNombre(uid);
                    } else {
                        elegirPersona(uid);
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void elegirPersona(String uid) {
        String[] nombres = new String[personas.length()];
        for (int indice = 0; indice < nombres.length; indice++) {
            JSONObject persona = personas.optJSONObject(indice);
            nombres[indice] = getString(R.string.persona_opcion,
                    persona.optString("nombre"), persona.optInt("id"));
        }
        dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.persona_existente)
                .setItems(nombres, (ventana, opcion) -> confirmarAsignacion(uid, personas.optJSONObject(opcion)))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void confirmarAsignacion(String uid, JSONObject persona) {
        String anterior = persona.optString("uid_rfid");
        int texto = anterior.isEmpty() || anterior.startsWith("SIN-")
                ? R.string.tarjeta_confirmar : R.string.tarjeta_reemplazar;
        dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.tarjeta_asignar)
                .setMessage(getString(texto, uid, persona.optString("nombre"), anterior))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.tarjeta_asignar,
                        (ventana, opcion) -> guardarPersona(uid, null, persona.optInt("id")))
                .show();
    }

    private void guardarPersona(@Nullable String uid, @Nullable String nombre, int personaId) {
        if (ocupada) {
            return;
        }
        View vista = requireView();
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("accion", uid == null ? "crear" : "asignar");
            if (uid != null) {
                cuerpo.put("uid_rfid", uid);
            }
            if (nombre != null) {
                cuerpo.put("nombre", nombre);
            } else {
                cuerpo.put("persona_id", personaId);
            }
        } catch (JSONException error) {
            Snackbar.make(vista, R.string.error_servidor, Snackbar.LENGTH_LONG).show();
            return;
        }

        habilitarAcciones(false);
        int mensajeExito = uid == null ? R.string.persona_creada : R.string.tarjeta_asignada;
        operacion.guardar(requireContext().getApplicationContext(), cuerpo, mensajeExito);
    }

    private void mostrarResultado(@Nullable OperacionPersonas.Resultado resultado) {
        if (resultado == null) {
            return;
        }
        operacion.consumirResultado();
        if (resultado.error != null && sesionVencida(resultado.error)) {
            return;
        }

        if (resultado.error != null) {
            boolean conflicto = resultado.error.networkResponse != null
                    && resultado.error.networkResponse.statusCode == 409;
            avisoOperacion = Snackbar.make(requireView(),
                    Api.mensajeDeError(requireContext(), resultado.error),
                    conflicto ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
            if (conflicto) {
                avisoOperacion.setAction(android.R.string.ok, v -> { });
            }
        } else {
            JSONObject respuesta = resultado.respuesta;
            int texto = respuesta.optBoolean("success") ? resultado.mensajeExito : R.string.error_servidor;
            avisoOperacion = Snackbar.make(requireView(),
                    respuesta.optString("mensaje", getString(texto)), Snackbar.LENGTH_LONG);
        }
        avisoOperacion.show();
        // La lista puede haber cambiado aunque se haya perdido la respuesta del POST.
        cargarListas();
    }

    private boolean sesionVencida(VolleyError error) {
        if (Api.esNoAutorizado(error)) {
            ManejoErrores.manejar(requireActivity(), null, error, null, true);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        if (consulta != null) {
            consulta.cancel();
            consulta = null;
        }
        if (dialogo != null) {
            dialogo.dismiss();
            dialogo = null;
        }
        if (avisoReintento != null) {
            avisoReintento.dismiss();
            avisoReintento = null;
        }
        if (avisoOperacion != null) {
            avisoOperacion.dismiss();
            avisoOperacion = null;
        }
        adaptador = null;
        super.onDestroyView();
    }
}
