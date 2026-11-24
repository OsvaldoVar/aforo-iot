package com.example.aforoiot;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class DispositivosAdapter extends RecyclerView.Adapter<DispositivosAdapter.Vista> {
    public interface Acciones {
        void copiar(Dispositivo dispositivo);

        void editar(Dispositivo dispositivo);

        void cambiarActivo(Dispositivo dispositivo, boolean activo);
    }

    private final List<Dispositivo> dispositivos = new ArrayList<>();
    private final Acciones acciones;

    public DispositivosAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void actualizar(List<Dispositivo> nuevos) {
        dispositivos.clear();
        dispositivos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_dispositivo, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista vista, int posicion) {
        Dispositivo dispositivo = dispositivos.get(posicion);

        vista.nombre.setText(dispositivo.getNombre());
        String ubicacion = dispositivo.getUbicacion();
        if (ubicacion == null || ubicacion.trim().isEmpty()) {
            vista.ubicacion.setText(R.string.sin_ubicacion);
        } else {
            vista.ubicacion.setText(ubicacion);
        }
        vista.apiKey.setText(vista.itemView.getContext().getString(
                R.string.api_key_formato, dispositivo.getApiKey()));
        actualizarChip(vista, dispositivo);

        vista.swActivo.setOnCheckedChangeListener(null);
        vista.swActivo.setChecked(dispositivo.isActivo());
        vista.swActivo.setOnCheckedChangeListener((boton, marcado) -> {
            if (marcado != dispositivo.isActivo()) {
                acciones.cambiarActivo(dispositivo, marcado);
            }
        });

        vista.copiar.setOnClickListener(v -> acciones.copiar(dispositivo));
        vista.editar.setOnClickListener(v -> acciones.editar(dispositivo));
    }

    private void actualizarChip(Vista vista, Dispositivo dispositivo) {
        int fondo;
        int contenido;
        int texto;

        if (!dispositivo.isActivo()) {
            fondo = R.color.surfaceContainer;
            contenido = R.color.onSurfaceVariant;
            texto = R.string.inactivo;
        } else if (dispositivo.isEnLinea()) {
            fondo = R.color.statusOnlineContainer;
            contenido = R.color.statusOnline;
            texto = R.string.en_linea;
        } else {
            fondo = R.color.errorContainer;
            contenido = R.color.onErrorContainer;
            texto = R.string.sin_conexion;
        }

        vista.estado.setText(texto);
        vista.estado.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(vista.itemView.getContext(), fondo)));
        vista.estado.setTextColor(ContextCompat.getColor(vista.itemView.getContext(), contenido));
    }

    @Override
    public int getItemCount() {
        return dispositivos.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView ubicacion;
        final TextView apiKey;
        final Chip estado;
        final MaterialButton copiar;
        final MaterialButton editar;
        final MaterialSwitch swActivo;

        Vista(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombreDispositivo);
            ubicacion = itemView.findViewById(R.id.txtUbicacionDispositivo);
            apiKey = itemView.findViewById(R.id.txtApiKeyDispositivo);
            estado = itemView.findViewById(R.id.chipEstadoDispositivo);
            copiar = itemView.findViewById(R.id.btnCopiarApiKey);
            editar = itemView.findViewById(R.id.btnEditarDispositivo);
            swActivo = itemView.findViewById(R.id.swActivo);
        }
    }
}
