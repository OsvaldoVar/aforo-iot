package com.example.aforoiot;

import android.os.Build;

public final class Config {
    private static final String URL_EMULADOR = "http://10.0.2.2:8080/api_aforo/";
    private static final String URL_CELULAR_USB = "http://127.0.0.1:8080/api_aforo/";

    // Override opcional en local.properties (no se sube a git):
    // aforo.baseUrl=http://192.168.0.114:8080/api_aforo/
    public static final String BASE_URL = !BuildConfig.URL_BASE_LOCAL.isEmpty()
            ? BuildConfig.URL_BASE_LOCAL
            : (esEmulador() ? URL_EMULADOR : URL_CELULAR_USB);

    private Config() {
    }

    private static boolean esEmulador() {
        return Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("Emulator");
    }
}
