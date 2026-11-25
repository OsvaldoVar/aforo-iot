package com.example.aforoiot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.net.URI;
import java.net.URISyntaxException;

public final class Config {
    private static final String PREFERENCIAS = "servidor_aforo";
    private static final String CLAVE_URL = "url";

    private static final String URL_EMULADOR = "http://10.0.2.2:8080/api_aforo/";
    private static final String URL_CELULAR_USB = "http://127.0.0.1:8080/api_aforo/";

    // Override opcional en local.properties (no se sube a git):
    // aforo.baseUrl=http://192.168.0.114:8080/api_aforo/
    public static final String BASE_URL = !BuildConfig.URL_BASE_LOCAL.isEmpty()
            ? BuildConfig.URL_BASE_LOCAL
            : (esEmulador() ? URL_EMULADOR : URL_CELULAR_USB);

    private Config() {
    }

    /**
     * URL base de la API. Usa la guardada desde la app (Login o Más → Servidor)
     * y, si no hay ninguna, la de configuracion (local.properties o la de fabrica).
     */
    public static String baseUrl(Context contexto) {
        String guardada = preferencias(contexto).getString(CLAVE_URL, "");
        return guardada == null || guardada.isEmpty() ? BASE_URL : guardada;
    }

    public static void guardarBaseUrl(Context contexto, String url) {
        SharedPreferences.Editor editor = preferencias(contexto).edit();
        if (url.equals(BASE_URL)) {
            editor.remove(CLAVE_URL);
        } else {
            editor.putString(CLAVE_URL, url);
        }
        editor.apply();
    }

    /**
     * Normaliza lo que escribe el usuario: agrega http:// si falta, la barra
     * final y el sufijo /api_aforo/. Devuelve null si no es una URL valida.
     */
    public static String normalizarBaseUrl(String texto) {
        if (texto == null) {
            return null;
        }
        String url = texto.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (url.contains("://") && !url.startsWith("http://") && !url.startsWith("https://")) {
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        if (!url.endsWith("/api_aforo/")) {
            url += "api_aforo/";
        }
        return url;
    }

    private static SharedPreferences preferencias(Context contexto) {
        return contexto.getApplicationContext()
                .getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
    }

    private static boolean esEmulador() {
        // En las pruebas locales Build.HARDWARE y Build.MODEL son null.
        String hardware = Build.HARDWARE == null ? "" : Build.HARDWARE;
        String modelo = Build.MODEL == null ? "" : Build.MODEL;
        return hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || modelo.contains("Emulator");
    }
}
