package com.example.aforoiot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PantallaVaciaFragment extends Fragment {
    private static final String ARG_ICONO = "icono";
    private static final String ARG_TITULO = "titulo";
    private static final String ARG_DETALLE = "detalle";

    public static PantallaVaciaFragment newInstance(int icono, int titulo, int detalle) {
        PantallaVaciaFragment fragmento = new PantallaVaciaFragment();
        Bundle argumentos = new Bundle();
        argumentos.putInt(ARG_ICONO, icono);
        argumentos.putInt(ARG_TITULO, titulo);
        argumentos.putInt(ARG_DETALLE, detalle);
        fragmento.setArguments(argumentos);
        return fragmento;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_pantalla_vacia, container, false);
        Bundle argumentos = requireArguments();

        ImageView icono = vista.findViewById(R.id.imgVacio);
        TextView titulo = vista.findViewById(R.id.txtTituloVacio);
        TextView detalle = vista.findViewById(R.id.txtDetalleVacio);

        icono.setImageResource(argumentos.getInt(ARG_ICONO));
        titulo.setText(argumentos.getInt(ARG_TITULO));
        detalle.setText(argumentos.getInt(ARG_DETALLE));
        return vista;
    }
}
