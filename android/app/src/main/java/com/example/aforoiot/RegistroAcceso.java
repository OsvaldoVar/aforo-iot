package com.example.aforoiot;

public class RegistroAcceso {
    private final String nombre;
    private final String tipo;
    private final String fecha;

    public RegistroAcceso(String nombre, String tipo, String fecha) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.fecha = fecha;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getFecha() {
        return fecha;
    }
}
