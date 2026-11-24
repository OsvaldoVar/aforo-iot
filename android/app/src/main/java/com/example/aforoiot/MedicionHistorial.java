package com.example.aforoiot;

public class MedicionHistorial {
    private final double temperatura;
    private final double humedad;
    private final String fecha;

    public MedicionHistorial(double temperatura, double humedad, String fecha) {
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.fecha = fecha;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public double getHumedad() {
        return humedad;
    }

    public String getFecha() {
        return fecha;
    }
}
