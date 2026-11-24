package com.example.aforoiot;

public class Dispositivo {
    private final int id;
    private final String nombre;
    private final String ubicacion;
    private final String apiKey;
    private final boolean activo;
    private final boolean enLinea;

    public Dispositivo(int id, String nombre, String ubicacion, String apiKey,
            boolean activo, boolean enLinea) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.apiKey = apiKey;
        this.activo = activo;
        this.enLinea = enLinea;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isActivo() {
        return activo;
    }

    public boolean isEnLinea() {
        return enLinea;
    }
}
