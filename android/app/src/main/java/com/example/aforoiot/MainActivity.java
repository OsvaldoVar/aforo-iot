package com.example.aforoiot;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String ESTADO_DESTINO = "destino";
    private static final int DESTINO_INICIAL = R.id.navDashboard;

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private SessionManager sesion;
    private int destinoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SessionManager(this);
        if (!sesion.estaLogueado()) {
            abrirLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottomNav);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.accionCerrarSesion) {
                confirmarCierreSesion();
                return true;
            }
            return false;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            mostrarPantalla(item.getItemId());
            return true;
        });

        if (savedInstanceState != null) {
            destinoActual = savedInstanceState.getInt(ESTADO_DESTINO, DESTINO_INICIAL);
            bottomNav.setSelectedItemId(destinoActual);
            toolbar.setTitle(tituloDe(destinoActual));
        } else {
            destinoActual = 0;
            bottomNav.setSelectedItemId(DESTINO_INICIAL);
        }
    }

    private void mostrarPantalla(int itemId) {
        if (itemId == destinoActual) {
            return;
        }

        Fragment pantalla;
        int titulo;
        if (itemId == R.id.navDashboard) {
            pantalla = new DashboardFragment();
            titulo = R.string.menu_dashboard;
        } else {
            int icono;
            int detalle;
            if (itemId == R.id.navHistorial) {
                icono = R.drawable.ic_historial;
                titulo = R.string.menu_historial;
                detalle = R.string.vacio_historial;
            } else if (itemId == R.id.navPersonas) {
                icono = R.drawable.ic_personas;
                titulo = R.string.menu_personas;
                detalle = R.string.vacio_personas;
            } else {
                icono = R.drawable.ic_mas;
                titulo = R.string.menu_mas;
                detalle = R.string.vacio_mas;
            }
            pantalla = PantallaVaciaFragment.newInstance(icono, titulo, detalle);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedorPantalla, pantalla)
                .commit();

        toolbar.setTitle(titulo);
        destinoActual = itemId;
    }

    private int tituloDe(int itemId) {
        if (itemId == R.id.navHistorial) {
            return R.string.menu_historial;
        }
        if (itemId == R.id.navPersonas) {
            return R.string.menu_personas;
        }
        if (itemId == R.id.navMas) {
            return R.string.menu_mas;
        }
        return R.string.menu_dashboard;
    }

    private void confirmarCierreSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cerrar_sesion)
                .setMessage(R.string.cerrar_sesion_confirmacion)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cerrar_sesion, (dialogo, opcion) -> cerrarSesion())
                .show();
    }

    private void cerrarSesion() {
        sesion.cerrarSesion();
        abrirLogin();
    }

    private void abrirLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_DESTINO, destinoActual);
    }
}
