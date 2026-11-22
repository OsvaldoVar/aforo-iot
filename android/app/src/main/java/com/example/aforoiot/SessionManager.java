package com.example.aforoiot;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String ARCHIVO = "sesion_aforo";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_NOMBRE = "nombre";

    private final SharedPreferences preferencias;

    public SessionManager(Context contexto) {
        preferencias = contexto.getApplicationContext()
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String token, String nombre) {
        preferencias.edit()
                .putString(CLAVE_TOKEN, token)
                .putString(CLAVE_NOMBRE, nombre)
                .apply();
    }

    public String obtenerToken() {
        return preferencias.getString(CLAVE_TOKEN, null);
    }

    public String obtenerNombre() {
        return preferencias.getString(CLAVE_NOMBRE, "");
    }

    public boolean estaLogueado() {
        return obtenerToken() != null;
    }

    public void cerrarSesion() {
        preferencias.edit()
                .remove(CLAVE_TOKEN)
                .remove(CLAVE_NOMBRE)
                .apply();
    }
}
