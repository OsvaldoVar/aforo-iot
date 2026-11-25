package com.example.aforoiot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

public class PersonasAdapter extends RecyclerView.Adapter<PersonasAdapter.Fila> {
    private static final int PERSONA = 0;
    private static final int TITULO_PENDIENTES = 1;
    private static final int TARJETA = 2;
    private static final int VACIO_PERSONAS = 3;
    private static final int VACIO_PENDIENTES = 4;

    interface AlAsignar {
        void asignar(String uid);
    }

    private final AlAsignar alAsignar;
    private JSONArray personas = new JSONArray();
    private JSONArray pendientes = new JSONArray();
    private boolean habilitado;

    PersonasAdapter(AlAsignar alAsignar) {
        this.alAsignar = alAsignar;
    }

    void actualizar(JSONArray personas, JSONArray pendientes) {
        this.personas = personas;
        this.pendientes = pendientes;
        notifyDataSetChanged();
    }

    void habilitar(boolean habilitado) {
        this.habilitado = habilitado;
        notifyDataSetChanged();
    }

    private int posicionTitulo() {
        return Math.max(1, personas.length());
    }

    @Override
    public int getItemCount() {
        return posicionTitulo() + 1 + Math.max(1, pendientes.length());
    }

    @Override
    public int getItemViewType(int posicion) {
        if (posicion < posicionTitulo()) {
            return personas.length() == 0 ? VACIO_PERSONAS : PERSONA;
        }
        if (posicion == posicionTitulo()) {
            return TITULO_PENDIENTES;
        }
        return pendientes.length() == 0 ? VACIO_PENDIENTES : TARJETA;
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        LayoutInflater inflador = LayoutInflater.from(padre.getContext());
        if (tipo == PERSONA || tipo == TARJETA) {
            int layout = tipo == PERSONA ? R.layout.item_persona : R.layout.item_tarjeta_pendiente;
            return new Fila(inflador.inflate(layout, padre, false));
        }
        return new Fila(inflador.inflate(R.layout.item_personas_texto, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        switch (getItemViewType(posicion)) {
            case PERSONA:
                mostrarPersona(fila, personas.optJSONObject(posicion));
                break;
            case TARJETA:
                mostrarTarjeta(fila, pendientes.optJSONObject(posicion - posicionTitulo() - 1));
                break;
            case TITULO_PENDIENTES:
                fila.titulo.setText(R.string.tarjetas_pendientes);
                break;
            case VACIO_PERSONAS:
                fila.titulo.setText(R.string.personas_sin_registros);
                break;
            default:
                fila.titulo.setText(R.string.tarjetas_sin_pendientes);
                break;
        }
    }

    private void mostrarPersona(Fila fila, JSONObject persona) {
        String uid = persona.optString("uid_rfid");
        fila.titulo.setText(persona.optString("nombre"));
        int estado = persona.optBoolean("activo") ? R.string.persona_activa : R.string.persona_inactiva;
        int tarjeta = uid.startsWith("SIN-") || uid.isEmpty()
                ? R.string.persona_sin_tarjeta : R.string.persona_uid;
        fila.detalle.setText(fila.itemView.getContext().getString(R.string.persona_detalle,
                fila.itemView.getContext().getString(estado),
                fila.itemView.getContext().getString(tarjeta, uid)));
    }

    private void mostrarTarjeta(Fila fila, JSONObject pendiente) {
        String uid = pendiente.optString("uid_rfid");
        fila.titulo.setText(R.string.tarjeta_sin_asignar);
        fila.detalle.setText(fila.itemView.getContext().getString(
                R.string.tarjeta_detalle, uid, pendiente.optString("fecha")));
        fila.asignar.setEnabled(habilitado);
        fila.asignar.setContentDescription(fila.itemView.getContext().getString(
                R.string.tarjeta_asignar_descripcion, uid));
        fila.asignar.setOnClickListener(vista -> alAsignar.asignar(uid));
    }

    static class Fila extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView detalle;
        final MaterialButton asignar;

        Fila(View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.txtTituloFila);
            detalle = vista.findViewById(R.id.txtDetalleFila);
            asignar = vista.findViewById(R.id.btnAsignar);
        }
    }
}
