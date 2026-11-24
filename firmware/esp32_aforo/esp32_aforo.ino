#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPI.h>
#include <MFRC522.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include <string.h>
#include <math.h>
#include "config.h"

#define SS_PIN 5
#define RST_PIN 22
#define SCK_PIN 18
#define MISO_PIN 19
#define MOSI_PIN 23
#define DHT_PIN 4
#define ANTIRREBOTE_RFID_MS 3000UL
#define CAPACIDAD_LECTURAS_RECIENTES 16
#define TAMANO_UID_HEX 21
#define INTERVALO_MEDICION 15000UL
#define INTERVALO_RECONEXION 10000UL
#define INTERVALO_REINTENTO_API 30000UL
#define TIEMPO_ESPERA_HTTP_MS 3000
#define CAPACIDAD_COLA 8
#define PILA_TAREA_IO_BYTES 8192

#define AVISO_COLA_ACCESO 1U
#define AVISO_COLA_MEDICION 2U
#define AVISO_TARJETAS_RECIENTES 4U
#define AVISO_HALT 8U

struct LecturaReciente {
    char uid[TAMANO_UID_HEX];
    unsigned long instante;
};

struct Trabajo {
    // Un UID vacío representa una medición programada.
    char uid[TAMANO_UID_HEX];
    unsigned long instante;
};

MFRC522 lector(SS_PIN, RST_PIN);
LecturaReciente lecturasRecientes[CAPACIDAD_LECTURAS_RECIENTES] = {};
DHT dht(DHT_PIN, DHT22);
unsigned long ultimaMedicion = 0;
QueueHandle_t colaTrabajos = nullptr;
TaskHandle_t tareaIo = nullptr;

// El DHT22 y el estado de red y recuperación solo se usan en la tarea de E/S.
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

void procesarRespuestaAcceso(int codigo, const String& cuerpo) {
    if (codigo <= 0) {
        Serial.printf("Acceso: error de comunicacion (%d). Sin confirmacion.\n", codigo);
        return;
    }
    if (codigo != HTTP_CODE_OK) {
        Serial.printf("Acceso: error HTTP %d. Sin confirmacion.\n", codigo);
        return;
    }

    JsonDocument respuesta;
    if (deserializeJson(respuesta, cuerpo) || !respuesta["success"].is<bool>()) {
        Serial.println("Acceso: respuesta JSON invalida. Sin confirmacion.");
        return;
    }

    if (respuesta["success"].as<bool>()) {
        const char* tipo = respuesta["tipo"] | "";
        if (strcmp(tipo, "entrada") == 0) {
            Serial.println("Acceso: entrada registrada.");
        } else if (strcmp(tipo, "salida") == 0) {
            Serial.println("Acceso: salida registrada.");
        } else {
            Serial.println("Acceso: respuesta sin tipo valido. Sin confirmacion.");
        }
        return;
    }

    const char* error = respuesta["error"] | "";
    if (strcmp(error, "tarjeta_no_registrada") == 0) {
        Serial.println("Acceso: tarjeta no registrada.");
    } else if (strcmp(error, "aforo_lleno") == 0) {
        Serial.println("Acceso: denegado por aforo lleno.");
    } else {
        Serial.println("Acceso: denegado sin motivo reconocido.");
    }
}

void enviarAcceso(const char* uid) {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("Acceso: sin WiFi. Retira y vuelve a presentar la tarjeta.");
        return;
    }

    WiFiClient cliente;
    HTTPClient http;
    if (!http.begin(cliente, String(SERVER) + "esp32/acceso.php")) {
        Serial.println("Acceso: no se pudo iniciar la solicitud. Revisa SERVER.");
        return;
    }

    http.setConnectTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.setTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.addHeader("Content-Type", "application/json");

    JsonDocument acceso;
    acceso["api_key"] = API_KEY;
    acceso["uid"] = uid;
    String cuerpo;
    serializeJson(acceso, cuerpo);
    const int codigo = http.POST(cuerpo);

    const String respuesta = codigo == HTTP_CODE_OK ? http.getString() : String();
    http.end();
    procesarRespuestaAcceso(codigo, respuesta);
}

bool registrarLectura(const char* uid, unsigned long ahora) {
    LecturaReciente* disponible = nullptr;
    for (LecturaReciente& lectura : lecturasRecientes) {
        if (lectura.uid[0] == '\0' || ahora - lectura.instante >= ANTIRREBOTE_RFID_MS) {
            disponible = &lectura;
        } else if (strcmp(lectura.uid, uid) == 0) {
            return false;
        }
    }

    if (disponible == nullptr) {
        xTaskNotify(tareaIo, AVISO_TARJETAS_RECIENTES, eSetBits);
        return false;
    }
    strcpy(disponible->uid, uid);
    disponible->instante = ahora;
    return true;
}

void consultarTarjeta() {
    if (!lector.PICC_IsNewCardPresent() || !lector.PICC_ReadCardSerial()) {
        return;
    }

    // REQA ignora las tarjetas en HALT hasta que vuelvan a entrar al campo.
    if (lector.PICC_HaltA() != MFRC522::STATUS_OK) {
        xTaskNotify(tareaIo, AVISO_HALT, eSetBits);
        return;
    }

    const byte longitud = lector.uid.size;
    if (longitud != 4 && longitud != 7 && longitud != 10) {
        return;
    }

    const char hex[] = "0123456789ABCDEF";
    Trabajo trabajo = {};
    for (byte indice = 0; indice < longitud; ++indice) {
        const byte valor = lector.uid.uidByte[indice];
        trabajo.uid[2 * indice] = hex[valor >> 4];
        trabajo.uid[2 * indice + 1] = hex[valor & 0x0F];
    }
    trabajo.instante = millis();
    if (registrarLectura(trabajo.uid, trabajo.instante)
            && xQueueSend(colaTrabajos, &trabajo, 0) != pdPASS) {
        xTaskNotify(tareaIo, AVISO_COLA_ACCESO, eSetBits);
    }
}

bool enviarMedicion() {
    const float temperatura = dht.readTemperature();
    const float humedad = dht.readHumidity();
    if (!isfinite(temperatura) || !isfinite(humedad)) {
        Serial.println("Lectura invalida del DHT22. Medicion omitida.");
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

void procesarMedicionProgramada(unsigned long programada) {
    const unsigned long ahora = millis();
    if (ahora - programada >= INTERVALO_MEDICION) {
        Serial.println("Medicion vencida en cola. Se omite.");
        return;
    }
    if (WiFi.status() != WL_CONNECTED) {
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

void informarAvisos() {
    uint32_t avisos = 0;
    if (xTaskNotifyWait(0, UINT32_MAX, &avisos, 0) != pdTRUE) {
        return;
    }
    if (avisos & AVISO_COLA_ACCESO) {
        Serial.println("Acceso omitido: cola llena. Retira y vuelve a presentar la tarjeta.");
    }
    if (avisos & AVISO_COLA_MEDICION) {
        Serial.println("Medicion omitida: cola llena.");
    }
    if (avisos & AVISO_TARJETAS_RECIENTES) {
        Serial.println("Acceso: demasiadas tarjetas recientes. Intenta de nuevo.");
    }
    if (avisos & AVISO_HALT) {
        Serial.println("Acceso: no se pudo dejar la tarjeta en reposo.");
    }
}

void tareaEntradaSalida(void*) {
    dht.begin();
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    Serial.println("Conectando a WiFi.");
    ultimoIntentoWifi = millis();

    for (;;) {
        informarAvisos();
        actualizarWifi(millis());
        Trabajo trabajo;
        if (xQueueReceive(colaTrabajos, &trabajo, pdMS_TO_TICKS(100)) == pdTRUE) {
            if (trabajo.uid[0] != '\0') {
                enviarAcceso(trabajo.uid);
            } else {
                procesarMedicionProgramada(trabajo.instante);
            }
        }
    }
}

void setup() {
    Serial.begin(115200);
    SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
    lector.PCD_Init();

    colaTrabajos = xQueueCreate(CAPACIDAD_COLA, sizeof(Trabajo));
    const BaseType_t nucleoIo = portNUM_PROCESSORS > 1 ? 1 - xPortGetCoreID() : tskNO_AFFINITY;
    if (colaTrabajos == nullptr
            || xTaskCreatePinnedToCore(tareaEntradaSalida, "aforo_io", PILA_TAREA_IO_BYTES,
                                       nullptr, 1, &tareaIo, nucleoIo) != pdPASS) {
        if (colaTrabajos != nullptr) {
            vQueueDelete(colaTrabajos);
            colaTrabajos = nullptr;
        }
        Serial.println("No se pudo iniciar la cola o la tarea de E/S.");
    }
    ultimaMedicion = millis();
}

void loop() {
    if (colaTrabajos == nullptr) {
        return;
    }
    consultarTarjeta();
    const unsigned long ahora = millis();
    if (ahora - ultimaMedicion >= INTERVALO_MEDICION) {
        ultimaMedicion = ahora;
        const Trabajo trabajo = {{}, ahora};
        if (xQueueSend(colaTrabajos, &trabajo, 0) != pdPASS) {
            xTaskNotify(tareaIo, AVISO_COLA_MEDICION, eSetBits);
        }
    }
}
