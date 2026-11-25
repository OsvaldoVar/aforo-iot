package com.example.aforoiot;

public class Alerta {
    private final int id;
    private final String tipo;
    private final String mensaje;
    private final String fecha;
    private final boolean atendida;

    public Alerta(int id, String tipo, String mensaje, String fecha, boolean atendida) {
        this.id = id;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.atendida = atendida;
    }

    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getFecha() {
        return fecha;
    }

    public boolean isAtendida() {
        return atendida;
    }
}
