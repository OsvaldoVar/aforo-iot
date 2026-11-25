package com.example.aforoiot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AccesosAdapter extends RecyclerView.Adapter<AccesosAdapter.Vista> {
    private final List<RegistroAcceso> registros = new ArrayList<>();

    public void actualizar(List<RegistroAcceso> nuevos) {
        registros.clear();
        registros.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_historial_acceso, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista vista, int posicion) {
        RegistroAcceso registro = registros.get(posicion);
        boolean entrada = "entrada".equalsIgnoreCase(registro.getTipo());

        vista.nombre.setText(registro.getNombre());
        vista.fecha.setText(FormatoFecha.fechaHora(registro.getFecha()));
        vista.tipo.setText(entrada ? R.string.entrada : R.string.salida);
        vista.tipo.setBackgroundResource(entrada
                ? R.drawable.bg_badge_entrada
                : R.drawable.bg_badge_salida);
        vista.tipo.setTextColor(ContextCompat.getColor(vista.itemView.getContext(),
                entrada ? R.color.onPrimaryContainer : R.color.onSecondaryContainer));
    }

    @Override
    public int getItemCount() {
        return registros.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView fecha;
        final TextView tipo;

        Vista(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombreAcceso);
            fecha = itemView.findViewById(R.id.txtFechaAcceso);
            tipo = itemView.findViewById(R.id.txtTipoAcceso);
        }
    }
}
