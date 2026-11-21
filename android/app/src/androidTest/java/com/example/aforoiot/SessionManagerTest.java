package com.example.aforoiot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SessionManagerTest {

    @Test
    public void laSesionPersisteEntreInstancias() {
        Context contexto = ApplicationProvider.getApplicationContext();
        SessionManager sesion = new SessionManager(contexto);
        sesion.cerrarSesion();

        sesion.guardarSesion("token-de-prueba", "Oscar");

        SessionManager nuevaInstancia = new SessionManager(contexto);
        assertEquals("token-de-prueba", nuevaInstancia.obtenerToken());
        assertEquals("Oscar", nuevaInstancia.obtenerNombre());
        assertTrue(nuevaInstancia.estaLogueado());

        nuevaInstancia.cerrarSesion();
        assertFalse(nuevaInstancia.estaLogueado());
        assertNull(nuevaInstancia.obtenerToken());
    }
}
