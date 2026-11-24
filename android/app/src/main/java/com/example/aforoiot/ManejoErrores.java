package com.example.aforoiot;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

public final class ManejoErrores {
    private ManejoErrores() {
    }

    public static void manejar(Activity actividad, View vista, VolleyError error,
            Runnable reintentar, boolean sesionRequerida) {
        if (actividad == null || actividad.isFinishing() || actividad.isDestroyed()) {
            return;
        }

        SessionManager sesion = new SessionManager(actividad);
        if (sesionRequerida && Api.esNoAutorizado(error)) {
            if (!sesion.estaLogueado()) {
                return;
            }
            sesion.cerrarSesion();
            Intent intent = new Intent(actividad, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            actividad.startActivity(intent);
            actividad.finish();
            return;
        }

        if (vista == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(vista, Api.mensajeDeError(actividad, error),
                Snackbar.LENGTH_LONG);
        if (reintentar != null) {
            snackbar.setAction(R.string.reintentar, v -> reintentar.run());
        }
        snackbar.show();
    }
}
