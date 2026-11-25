package com.example.aforoiot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.Vista> {
    public interface Acciones {
        void atender(Alerta alerta);
    }

    private final List<Alerta> alertas = new ArrayList<>();
    private final Acciones acciones;

    public AlertasAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void actualizar(List<Alerta> nuevas) {
        alertas.clear();
        alertas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_alerta, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista vista, int posicion) {
        Alerta alerta = alertas.get(posicion);
        vista.tipo.setText(textoTipo(vista, alerta.getTipo()));
        vista.mensaje.setText(alerta.getMensaje());
        vista.fecha.setText(FormatoFecha.fechaHora(alerta.getFecha()));
        vista.estado.setText(alerta.isAtendida()
                ? R.string.alerta_atendida
                : R.string.alerta_pendiente);
        vista.atender.setVisibility(alerta.isAtendida() ? View.GONE : View.VISIBLE);
        vista.atender.setOnClickListener(v -> acciones.atender(alerta));
    }

    private int textoTipo(Vista vista, String tipo) {
        if ("temperatura".equalsIgnoreCase(tipo)) {
            return R.string.tipo_temperatura;
        }
        if ("humedad".equalsIgnoreCase(tipo)) {
            return R.string.tipo_humedad;
        }
        if ("aforo".equalsIgnoreCase(tipo)) {
            return R.string.tipo_aforo;
        }
        return R.string.tipo_alerta;
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView tipo;
        final TextView mensaje;
        final TextView fecha;
        final TextView estado;
        final MaterialButton atender;

        Vista(@NonNull View itemView) {
            super(itemView);
            tipo = itemView.findViewById(R.id.txtTipoAlerta);
            mensaje = itemView.findViewById(R.id.txtMensajeAlerta);
            fecha = itemView.findViewById(R.id.txtFechaAlerta);
            estado = itemView.findViewById(R.id.txtEstadoAlerta);
            atender = itemView.findViewById(R.id.btnAtenderAlerta);
        }
    }
}
