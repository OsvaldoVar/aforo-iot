package com.example.aforoiot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MedicionesAdapter extends RecyclerView.Adapter<MedicionesAdapter.Vista> {
    private final List<MedicionHistorial> mediciones = new ArrayList<>();

    public void actualizar(List<MedicionHistorial> nuevas) {
        mediciones.clear();
        mediciones.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_historial_medicion, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista vista, int posicion) {
        MedicionHistorial medicion = mediciones.get(posicion);
        vista.valores.setText(vista.itemView.getContext().getString(
                R.string.medicion_formato, medicion.getTemperatura(), medicion.getHumedad()));
        vista.fecha.setText(FormatoFecha.fechaHora(medicion.getFecha()));
    }

    @Override
    public int getItemCount() {
        return mediciones.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView valores;
        final TextView fecha;

        Vista(@NonNull View itemView) {
            super(itemView);
            valores = itemView.findViewById(R.id.txtValoresMedicion);
            fecha = itemView.findViewById(R.id.txtFechaMedicion);
        }
    }
}
