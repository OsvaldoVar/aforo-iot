#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <math.h>
#include <string.h>
#include "config.h"

#define DHT_PIN 4
#define LED_VERDE 25
#define LED_ROJO 26
#define BUZZER 27
#define DURACION_INDICADOR_MS 2000UL
#define DURACION_PITIDO_MS 100UL
#define FRECUENCIA_BUZZER_HZ 2000
#define INTERVALO_MEDICION 15000UL
#define INTERVALO_RECONEXION 10000UL
#define TIEMPO_ESPERA_HTTP_MS 3000

DHT dht(DHT_PIN, DHT22);
unsigned long ultimaMedicion = 0;
unsigned long ultimoIntentoWifi = 0;
bool wifiConectado = false;

unsigned long inicioIndicador = 0;
bool indicadorActivo = false;
bool indicadorDenegado = false;
bool buzzerActivo = false;

void ajustarBuzzer(bool sonar) {
    if (sonar == buzzerActivo) {
        return;
    }
    buzzerActivo = sonar;
    if (sonar) {
        tone(BUZZER, FRECUENCIA_BUZZER_HZ);
    } else {
        noTone(BUZZER);
    }
}

void actualizarIndicador(unsigned long ahora) {
    if (!indicadorActivo) {
        return;
    }

    const unsigned long transcurrido = ahora - inicioIndicador;
    if (transcurrido >= DURACION_INDICADOR_MS) {
        digitalWrite(LED_VERDE, LOW);
        digitalWrite(LED_ROJO, LOW);
        ajustarBuzzer(false);
        indicadorActivo = false;
        return;
    }

    // Dos pulsos de 100 ms separados por 100 ms de silencio.
    ajustarBuzzer(indicadorDenegado && (transcurrido < DURACION_PITIDO_MS
            || (transcurrido >= 2 * DURACION_PITIDO_MS && transcurrido < 3 * DURACION_PITIDO_MS)));
}

void indicarRespuestaAcceso(int codigoHttp, const String& cuerpo) {
    JsonDocument respuesta;
    bool permitido = false;
    if (codigoHttp == HTTP_CODE_OK && !deserializeJson(respuesta, cuerpo)
            && respuesta["success"].is<bool>() && respuesta["success"].as<bool>()) {
        const char* tipo = respuesta["tipo"] | "";
        permitido = strcmp(tipo, "entrada") == 0 || strcmp(tipo, "salida") == 0;
    }

    // Apaga la señal anterior antes de iniciar la nueva.
    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, LOW);
    ajustarBuzzer(false);

    inicioIndicador = millis();
    indicadorDenegado = !permitido;
    indicadorActivo = true;
    digitalWrite(permitido ? LED_VERDE : LED_ROJO, HIGH);
    actualizarIndicador(inicioIndicador);
}

void actualizarWifi(unsigned long ahora) {
    const bool conectado = WiFi.status() == WL_CONNECTED;
    if (conectado != wifiConectado) {
        Serial.println(conectado ? "WiFi conectado." : "Conexion WiFi perdida.");
        wifiConectado = conectado;
    }

    if (!conectado && ahora - ultimoIntentoWifi >= INTERVALO_RECONEXION) {
        ultimoIntentoWifi = ahora;
        Serial.println("Reintentando conexion WiFi.");
        WiFi.reconnect();
    }
}

void enviarMedicion() {
    const float temperatura = dht.readTemperature();
    const float humedad = dht.readHumidity();
    if (!isfinite(temperatura) || !isfinite(humedad)) {
        Serial.println("Lectura invalida del DHT22. Medicion omitida.");
        return;
    }

    WiFiClient cliente;
    HTTPClient http;
    const String url = String(SERVER) + "esp32/medicion.php";
    if (!http.begin(cliente, url)) {
        Serial.println("No se pudo iniciar la solicitud. Revisa SERVER.");
        return;
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

    if (codigo == HTTP_CODE_OK) {
        JsonDocument respuesta;
        const DeserializationError error = deserializeJson(respuesta, http.getString());
        if (error) {
            Serial.println("HTTP 200 con respuesta JSON invalida.");
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
    } else {
        Serial.printf("Error HTTP %d al enviar la medicion.\n", codigo);
    }

    http.end();
}

void setup() {
    Serial.begin(115200);
    pinMode(LED_VERDE, OUTPUT);
    pinMode(LED_ROJO, OUTPUT);
    pinMode(BUZZER, OUTPUT);
    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, LOW);
    digitalWrite(BUZZER, LOW);
    dht.begin();

    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    Serial.println("Conectando a WiFi.");
    ultimaMedicion = ultimoIntentoWifi = millis();
}

void loop() {
    const unsigned long ahora = millis();
    actualizarIndicador(ahora);
    actualizarWifi(ahora);

    if (ahora - ultimaMedicion >= INTERVALO_MEDICION) {
        ultimaMedicion = ahora;
        if (wifiConectado) {
            enviarMedicion();
        } else {
            Serial.println("Sin WiFi. Medicion omitida.");
        }
    }
}
