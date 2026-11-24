package com.example.aforoiot;

public class PersonaDentro {
    private final String nombre;
    private final String horaEntrada;

    public PersonaDentro(String nombre, String horaEntrada) {
        this.nombre = nombre;
        this.horaEntrada = horaEntrada;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHoraEntrada() {
        return horaEntrada;
    }
}
