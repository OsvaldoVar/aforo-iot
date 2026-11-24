package com.example.aforoiot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PersonasDentroAdapter extends RecyclerView.Adapter<PersonasDentroAdapter.Vista> {
    private final List<PersonaDentro> personas = new ArrayList<>();

    public void actualizar(List<PersonaDentro> nuevas) {
        personas.clear();
        personas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_dentro, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista vista, int posicion) {
        PersonaDentro persona = personas.get(posicion);
        vista.nombre.setText(persona.getNombre());

        String hora = persona.getHoraEntrada();
        String horaCorta = hora.length() >= 16 ? hora.substring(11, 16) : hora;
        vista.hora.setText(vista.itemView.getContext()
                .getString(R.string.desde_hora, horaCorta));
    }

    @Override
    public int getItemCount() {
        return personas.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView hora;

        Vista(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombrePersona);
            hora = itemView.findViewById(R.id.txtHoraEntrada);
        }
    }
}
