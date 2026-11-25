package com.example.aforoiot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ConfigTest {
    @Test
    public void agregaEsquemaBarraYSufijo() {
        assertEquals("http://192.168.0.102:8080/api_aforo/",
                Config.normalizarBaseUrl("192.168.0.102:8080"));
        assertEquals("http://192.168.0.102:8080/api_aforo/",
                Config.normalizarBaseUrl("http://192.168.0.102:8080"));
        assertEquals("http://192.168.0.102:8080/api_aforo/",
                Config.normalizarBaseUrl("http://192.168.0.102:8080/api_aforo"));
    }

    @Test
    public void conservaUnaUrlCompleta() {
        assertEquals("https://api.example.com/api_aforo/",
                Config.normalizarBaseUrl("https://api.example.com/api_aforo/"));
    }

    @Test
    public void rechazaValoresInvalidos() {
        assertNull(Config.normalizarBaseUrl(null));
        assertNull(Config.normalizarBaseUrl(""));
        assertNull(Config.normalizarBaseUrl("ftp://host/"));
        assertNull(Config.normalizarBaseUrl("http://"));
    }
}
