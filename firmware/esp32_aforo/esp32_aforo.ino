#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <math.h>
#include "config.h"

#define DHT_PIN 4
#define INTERVALO_MEDICION 15000UL
#define INTERVALO_RECONEXION 10000UL
#define INTERVALO_REINTENTO_API 30000UL
#define TIEMPO_ESPERA_HTTP_MS 3000

// El kit del proyecto trae un DHT11. Para usar un DHT22, define DHT_TIPO DHT22 en config.h.
#ifndef DHT_TIPO
#define DHT_TIPO DHT11
#endif

DHT dht(DHT_PIN, DHT_TIPO);
unsigned long ultimaMedicion = 0;
unsigned long ultimoIntentoWifi = 0;
bool wifiConectado = false;
unsigned long ultimoFalloApi = 0;
bool apiEnPausa = false;

void actualizarWifi(unsigned long ahora) {
    const bool conectado = WiFi.status() == WL_CONNECTED;
    if (conectado != wifiConectado) {
        Serial.println(conectado ? "WiFi conectado." : "Conexion WiFi perdida.");
        wifiConectado = conectado;
    }

    if (!conectado && ahora - ultimoIntentoWifi >= INTERVALO_RECONEXION) {
        ultimoIntentoWifi = ahora;
        Serial.println("Reintentando conexion WiFi.");
        if (!WiFi.reconnect()) {
            WiFi.begin(WIFI_SSID, WIFI_PASS);
        }
    }
}

bool enviarMedicion() {
    const float temperatura = dht.readTemperature();
    const float humedad = dht.readHumidity();
    if (!isfinite(temperatura) || !isfinite(humedad)) {
        Serial.println("Lectura invalida del sensor DHT. Medicion omitida.");
        return false;
    }

    WiFiClient cliente;
    HTTPClient http;
    const String url = String(SERVER) + "esp32/medicion.php";
    if (!http.begin(cliente, url)) {
        Serial.println("No se pudo iniciar la solicitud. Revisa SERVER.");
        return false;
    }

    http.setConnectTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.setTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.addHeader("Content-Type", "application/json");

    JsonDocument medicion;
    medicion["api_key"] = API_KEY;
    medicion["temperatura"] = temperatura;
    medicion["humedad"] = humedad;

    String cuerpo;
    serializeJson(medicion, cuerpo);
    const int codigo = http.POST(cuerpo);

    bool falloTemporal = codigo <= 0 || codigo == 408 || codigo == 429
            || (codigo >= 500 && codigo < 600);
    if (codigo == HTTP_CODE_OK) {
        JsonDocument respuesta;
        const DeserializationError error = deserializeJson(respuesta, http.getString());
        if (error) {
            Serial.println("HTTP 200 con respuesta JSON invalida.");
            falloTemporal = true;
        } else if (respuesta["success"].is<bool>() && respuesta["success"].as<bool>()) {
            Serial.printf("Medicion registrada: %.1f C, %.1f %% (HTTP 200)\n",
                          temperatura, humedad);
        } else {
            Serial.println("HTTP 200 sin confirmacion de registro.");
        }
    } else if (codigo == HTTP_CODE_BAD_REQUEST) {
        Serial.println("HTTP 400: datos rechazados.");
    } else if (codigo == HTTP_CODE_UNAUTHORIZED) {
        Serial.println("HTTP 401: revisa API_KEY.");
    } else if (codigo <= 0) {
        Serial.printf("Error de red al enviar la medicion (%d).\n", codigo);
    } else if (falloTemporal) {
        Serial.printf("Fallo temporal de la API (HTTP %d).\n", codigo);
    } else {
        Serial.printf("Error HTTP %d al enviar la medicion.\n", codigo);
    }

    http.end();
    return falloTemporal;
}

void setup() {
    Serial.begin(115200);
    dht.begin();

    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    Serial.println("Conectando a WiFi.");
    ultimaMedicion = ultimoIntentoWifi = millis();
}

void loop() {
    const unsigned long ahora = millis();
    actualizarWifi(ahora);

    if (ahora - ultimaMedicion < INTERVALO_MEDICION) {
        return;
    }
    ultimaMedicion = ahora;

    if (!wifiConectado) {
        Serial.println("Sin WiFi. Medicion omitida.");
        return;
    }
    if (apiEnPausa && ahora - ultimoFalloApi < INTERVALO_REINTENTO_API) {
        return;
    }
    if (apiEnPausa) {
        Serial.println("Reintentando comunicacion con una nueva medicion.");
    }

    // Cada POST usa una lectura nueva. El servidor pudo guardar la anterior.
    apiEnPausa = enviarMedicion();
    if (apiEnPausa) {
        ultimoFalloApi = millis();
        Serial.println("Proximo intento con una nueva medicion tras al menos 30 s.");
    }
}
