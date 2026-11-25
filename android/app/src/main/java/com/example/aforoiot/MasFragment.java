package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

public class MasFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);
        vista.findViewById(R.id.cardDispositivos).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DispositivosActivity.class)));
        vista.findViewById(R.id.cardAlertas).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AlertasActivity.class)));
        vista.findViewById(R.id.cardLimites).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LimitesActivity.class)));
        vista.findViewById(R.id.cardGraficas).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GraficasActivity.class)));
        vista.findViewById(R.id.cardServidor).setOnClickListener(v ->
                DialogoServidor.mostrar(requireContext(), () ->
                        Snackbar.make(vista, R.string.servidor_guardado, Snackbar.LENGTH_LONG).show()));
    }
}
