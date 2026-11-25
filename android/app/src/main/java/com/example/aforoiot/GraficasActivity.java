package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraficasActivity extends AppCompatActivity {
    private static final long UN_DIA_MS = 24L * 60L * 60L * 1000L;
    private static final SimpleDateFormat FORMATO_FECHA =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat FORMATO_DIA =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat FORMATO_HORA =
            new SimpleDateFormat("HH:mm", Locale.US);

    private LineChart lineChart;
    private BarChart barChart;
    private TextView txtSinGraficas;
    private SessionManager sesion;
    private boolean medicionesListas;
    private boolean accesosListos;
    private boolean hayMediciones;
    private boolean hayAccesos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SessionManager(this);
        if (!sesion.estaLogueado()) {
            abrirLogin();
            return;
        }

        setContentView(R.layout.activity_graficas);
        MaterialToolbar toolbar = findViewById(R.id.toolbarGraficas);
        toolbar.setNavigationOnClickListener(v -> finish());

        lineChart = findViewById(R.id.lineChart);
        barChart = findViewById(R.id.barChart);
        txtSinGraficas = findViewById(R.id.txtSinGraficas);

        configurarGraficas();
        cargarMediciones();
        cargarAccesos();
    }

    private void configurarGraficas() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getLegend().setWordWrapEnabled(true);

        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getLegend().setEnabled(false);
    }

    private void cargarMediciones() {
        Api.get(this, "app/historial_mediciones.php?desde=" + desde() + "&hasta=" + hoy()
                        + "&orden=asc&limite=1000",
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        medicionesListas = true;
                        actualizarVacio();
                        return;
                    }
                    dibujarClima(respuesta.optJSONArray("mediciones"));
                    medicionesListas = true;
                    actualizarVacio();
                },
                error -> {
                    medicionesListas = true;
                    actualizarVacio();
                    ManejoErrores.manejar(this, findViewById(android.R.id.content),
                            error, this::cargarMediciones, true);
                });
    }

    private void dibujarClima(JSONArray arreglo) {
        List<Entry> temperaturas = new ArrayList<>();
        List<Entry> humedades = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        long limite = System.currentTimeMillis() - UN_DIA_MS;

        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject item = arreglo.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                Date fecha = parsear(item.optString("fecha"));
                if (fecha == null || fecha.getTime() < limite) {
                    continue;
                }
                int indice = temperaturas.size();
                temperaturas.add(new Entry(indice, (float) item.optDouble("temperatura", 0)));
                humedades.add(new Entry(indice, (float) item.optDouble("humedad", 0)));
                etiquetas.add(FORMATO_HORA.format(fecha));
            }
        }

        hayMediciones = !temperaturas.isEmpty();
        if (hayMediciones) {
            LineDataSet datosTemperatura = new LineDataSet(temperaturas,
                    getString(R.string.temperatura));
            datosTemperatura.setColor(ContextCompat.getColor(this, R.color.primary));
            datosTemperatura.setCircleColor(ContextCompat.getColor(this, R.color.primary));
            datosTemperatura.setLineWidth(2f);
            datosTemperatura.setCircleRadius(3f);

            LineDataSet datosHumedad = new LineDataSet(humedades, getString(R.string.humedad));
            datosHumedad.setColor(ContextCompat.getColor(this, R.color.tertiary));
            datosHumedad.setCircleColor(ContextCompat.getColor(this, R.color.tertiary));
            datosHumedad.setLineWidth(2f);
            datosHumedad.setCircleRadius(3f);

            lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
            lineChart.getXAxis().setLabelCount(Math.min(etiquetas.size(), 6));
            lineChart.setData(new LineData(datosTemperatura, datosHumedad));
            lineChart.invalidate();
        } else {
            lineChart.clear();
        }
        lineChart.setVisibility(hayMediciones ? View.VISIBLE : View.GONE);
    }

    private void cargarAccesos() {
        Api.get(this, "app/historial_accesos.php?desde=" + desde() + "&hasta=" + hoy()
                        + "&limite=1000",
                respuesta -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!respuesta.optBoolean("success", false)) {
                        accesosListos = true;
                        actualizarVacio();
                        return;
                    }
                    dibujarEntradas(respuesta.optJSONArray("registros"));
                    accesosListos = true;
                    actualizarVacio();
                },
                error -> {
                    accesosListos = true;
                    actualizarVacio();
                    ManejoErrores.manejar(this, findViewById(android.R.id.content),
                            error, this::cargarAccesos, true);
                });
    }

    private void dibujarEntradas(JSONArray arreglo) {
        int[] porHora = new int[24];
        long limite = System.currentTimeMillis() - UN_DIA_MS;

        if (arreglo != null) {
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject item = arreglo.optJSONObject(i);
                if (item == null || !"entrada".equalsIgnoreCase(item.optString("tipo"))) {
                    continue;
                }
                Date fecha = parsear(item.optString("fecha"));
                if (fecha == null || fecha.getTime() < limite) {
                    continue;
                }
                Calendar calendario = Calendar.getInstance();
                calendario.setTime(fecha);
                porHora[calendario.get(Calendar.HOUR_OF_DAY)]++;
            }
        }

        List<BarEntry> entradas = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        hayAccesos = false;
        for (int hora = 0; hora < porHora.length; hora++) {
            entradas.add(new BarEntry(hora, porHora[hora]));
            etiquetas.add(String.format(Locale.US, "%02d", hora));
            if (porHora[hora] > 0) {
                hayAccesos = true;
            }
        }

        BarDataSet datos = new BarDataSet(entradas, getString(R.string.entradas));
        datos.setColor(ContextCompat.getColor(this, R.color.primary));
        datos.setDrawValues(false);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(etiquetas));
        barChart.getXAxis().setLabelCount(6);
        barChart.setData(new BarData(datos));
        barChart.invalidate();
        barChart.setVisibility(hayAccesos ? View.VISIBLE : View.GONE);
    }

    private void actualizarVacio() {
        if (!medicionesListas || !accesosListos) {
            return;
        }
        txtSinGraficas.setVisibility(!hayMediciones && !hayAccesos
                ? View.VISIBLE
                : View.GONE);
    }

    private Date parsear(String fecha) {
        try {
            return FORMATO_FECHA.parse(fecha);
        } catch (Exception e) {
            return null;
        }
    }

    private String hoy() {
        return FORMATO_DIA.format(new Date());
    }

    private String desde() {
        Calendar calendario = Calendar.getInstance();
        calendario.add(Calendar.DAY_OF_YEAR, -1);
        return FORMATO_DIA.format(calendario.getTime());
    }

    private void abrirLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
