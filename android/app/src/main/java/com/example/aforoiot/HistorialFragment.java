package com.example.aforoiot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HistorialFragment extends Fragment {
    private static final String RUTA_ACCESOS = "app/historial_accesos.php";
    private static final String RUTA_MEDICIONES = "app/historial_mediciones.php";

    private TabLayout tabsHistorial;
    private MaterialButton btnRango;
    private RecyclerView rvHistorial;
    private View layoutVacio;
    private AccesosAdapter accesosAdapter;
    private MedicionesAdapter medicionesAdapter;

    private String desde;
    private String hasta;
    private int idPeticion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        tabsHistorial = vista.findViewById(R.id.tabsHistorial);
        btnRango = vista.findViewById(R.id.btnRango);
        rvHistorial = vista.findViewById(R.id.rvHistorial);
        layoutVacio = vista.findViewById(R.id.layoutVacio);

        accesosAdapter = new AccesosAdapter();
        medicionesAdapter = new MedicionesAdapter();
        rvHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistorial.setAdapter(accesosAdapter);

        btnRango.setOnClickListener(v -> abrirSelectorRango());
        tabsHistorial.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab pestana) {
                rvHistorial.setAdapter(pestana.getPosition() == 0
                        ? accesosAdapter
                        : medicionesAdapter);
                layoutVacio.setVisibility(View.GONE);
                cargarHistorial();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab pestana) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab pestana) {
            }
        });

        cargarHistorial();
    }

    private void cargarHistorial() {
        if (!isAdded()) {
            return;
        }
        final int peticion = ++idPeticion;
        final boolean esAccesos = tabsHistorial.getSelectedTabPosition() != 1;

        StringBuilder ruta = new StringBuilder(esAccesos ? RUTA_ACCESOS : RUTA_MEDICIONES)
                .append("?limite=100");
        if (desde != null && hasta != null) {
            ruta.append("&desde=").append(desde).append("&hasta=").append(hasta);
        }
        if (!esAccesos) {
            ruta.append("&orden=desc");
        }

        Api.get(requireContext(), ruta.toString(),
                respuesta -> {
                    if (!isAdded() || peticion != idPeticion) {
                        return;
                    }
                    if (esAccesos) {
                        mostrarAccesos(respuesta);
                    } else {
                        mostrarMediciones(respuesta);
                    }
                },
                error -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (Api.esNoAutorizado(error)) {
                        ManejoErrores.manejar(requireActivity(), getView(), error, null, true);
                        return;
                    }
                    if (peticion != idPeticion) {
                        return;
                    }
                    manejarError(error);
                });
    }

    private void mostrarAccesos(JSONObject respuesta) {
        if (!respuesta.optBoolean("success", false)) {
            mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
            return;
        }

        List<RegistroAcceso> registros = new ArrayList<>();
        JSONArray arreglo = respuesta.optJSONArray("registros");
        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject item = arreglo.optJSONObject(i);
                if (item != null) {
                    registros.add(new RegistroAcceso(
                            item.optString("nombre"),
                            item.optString("tipo"),
                            item.optString("fecha")));
                }
            }
        }
        accesosAdapter.actualizar(registros);
        mostrarVacio(registros.isEmpty());
    }

    private void mostrarMediciones(JSONObject respuesta) {
        if (!respuesta.optBoolean("success", false)) {
            mostrarMensaje(respuesta.optString("mensaje", getString(R.string.error_servidor)));
            return;
        }

        List<MedicionHistorial> mediciones = new ArrayList<>();
        JSONArray arreglo = respuesta.optJSONArray("mediciones");
        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject item = arreglo.optJSONObject(i);
                if (item != null) {
                    mediciones.add(new MedicionHistorial(
                            item.optDouble("temperatura", 0),
                            item.optDouble("humedad", 0),
                            item.optString("fecha")));
                }
            }
        }
        medicionesAdapter.actualizar(mediciones);
        mostrarVacio(mediciones.isEmpty());
    }

    private void mostrarVacio(boolean vacio) {
        layoutVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvHistorial.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void abrirSelectorRango() {
        MaterialDatePicker<Pair<Long, Long>> selector = MaterialDatePicker.Builder
                .dateRangePicker()
                .setTitleText(R.string.seleccionar_rango)
                .build();

        selector.addOnPositiveButtonClickListener(rango -> {
            if (rango.first == null || rango.second == null) {
                return;
            }
            desde = formatearDia(rango.first);
            hasta = formatearDia(rango.second);
            btnRango.setText(getString(R.string.rango_fechas, desde, hasta));
            cargarHistorial();
        });
        selector.show(getParentFragmentManager(), "rango_historial");
    }

    private String formatearDia(long milisegundos) {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(new Date(milisegundos));
    }

    private void manejarError(VolleyError error) {
        ManejoErrores.manejar(requireActivity(), getView(), error, this::cargarHistorial, true);
    }

    private void mostrarMensaje(String mensaje) {
        View vista = getView();
        if (vista != null) {
            Snackbar.make(vista, mensaje, Snackbar.LENGTH_LONG).show();
        }
    }
}
