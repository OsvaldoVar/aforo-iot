// Firmware del nodo ESP32 de Aforo IoT.
//
// Integra la lectura RFID con antirrebote (#43), las mediciones periodicas
// (#44), la senalizacion con LEDs y buzzer (#45), la consulta de comandos
// remotos (#78) y las alertas de aforo lleno y de medicion (#79).
// La red y el servidor se configuran en config.h.

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPI.h>
#include <MFRC522.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include <math.h>
#include <string.h>

#include "config.h"

// ---------- pines ----------
#define SS_PIN 5
#define RST_PIN 22
#define SCK_PIN 18
#define MISO_PIN 19
#define MOSI_PIN 23
#define DHT_PIN 4
#define LED_VERDE 25
#define LED_ROJO 26
#define BUZZER 27

// El kit del proyecto trae un DHT11. Para un DHT22, define DHT_TIPO DHT22 en config.h.
#ifndef DHT_TIPO
#define DHT_TIPO DHT11
#endif

// ---------- tiempos ----------
#define ANTIRREBOTE_RFID_MS 3000UL
#define INTERVALO_MEDICION 15000UL
#define INTERVALO_COMANDO_MS 3000UL
#define INTERVALO_RECONEXION 10000UL
#define INTERVALO_REINTENTO_API 30000UL
#define TIEMPO_ESPERA_HTTP_MS 3000
#define DURACION_ACCESO_MS 2000UL
#define DURACION_ABRIR_MS 3000UL
#define DURACION_ALARMA_MS 3000UL
#define DURACION_PITIDO_MS 100UL
#define FRECUENCIA_BUZZER_HZ 2000

// ---------- limites ----------
#define CAPACIDAD_LECTURAS_RECIENTES 16
#define CAPACIDAD_COLA 8
#define CAPACIDAD_SENALES 4
#define PILA_TAREA_IO_BYTES 8192
#define TAMANO_UID_HEX 21

#define AVISO_COLA_ACCESO 1U
#define AVISO_COLA_MEDICION 2U
#define AVISO_TARJETAS_RECIENTES 4U
#define AVISO_HALT 8U

enum Senal : uint8_t { SENAL_PERMITIDO, SENAL_DENEGADO, SENAL_ABRIR, SENAL_ALARMA };

struct LecturaReciente {
    char uid[TAMANO_UID_HEX];
    unsigned long instante;
};

struct Trabajo {
    // Un UID vacio representa una medicion programada.
    char uid[TAMANO_UID_HEX];
    unsigned long instante;
};

DHT dht(DHT_PIN, DHT_TIPO);
MFRC522 lector(SS_PIN, RST_PIN);
LecturaReciente lecturasRecientes[CAPACIDAD_LECTURAS_RECIENTES] = {};

QueueHandle_t colaTrabajos = nullptr;
QueueHandle_t colaSenales = nullptr;
TaskHandle_t tareaIo = nullptr;

unsigned long ultimaMedicion = 0;
unsigned long ultimoIntentoWifi = 0;
unsigned long ultimoFalloApi = 0;
bool wifiConectado = false;
bool apiEnPausa = false;

// Indicador LED/buzzer, solo en loop().
unsigned long inicioIndicador = 0;
unsigned long duracionIndicador = 0;
Senal senalActual = SENAL_PERMITIDO;
bool indicadorActivo = false;
bool buzzerActivo = false;

String urlApi(const char* ruta) {
    return String(SERVER) + ruta;
}

// ---------- WiFi ----------

void iniciarWifi() {
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    WiFi.begin(WIFI_SSID, WIFI_PASS);
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
        if (!WiFi.reconnect()) {
            iniciarWifi();
        }
    }
}

// ---------- indicador LED/buzzer (solo loop) ----------

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

void apagarSalidas() {
    digitalWrite(LED_VERDE, LOW);
    digitalWrite(LED_ROJO, LOW);
    ajustarBuzzer(false);
}

void actualizarIndicador(unsigned long ahora) {
    if (!indicadorActivo) {
        return;
    }
    const unsigned long transcurrido = ahora - inicioIndicador;
    if (transcurrido >= duracionIndicador) {
        apagarSalidas();
        indicadorActivo = false;
        return;
    }

    switch (senalActual) {
        case SENAL_DENEGADO:
            // Dos pulsos de 100 ms separados por 100 ms de silencio.
            ajustarBuzzer(transcurrido < DURACION_PITIDO_MS
                          || (transcurrido >= 2 * DURACION_PITIDO_MS
                              && transcurrido < 3 * DURACION_PITIDO_MS));
            break;
        case SENAL_ALARMA:
            ajustarBuzzer(true);
            break;
        default:
            ajustarBuzzer(false);
            break;
    }
}

void iniciarSenal(Senal senal, unsigned long ahora) {
    apagarSalidas();
    senalActual = senal;
    switch (senal) {
        case SENAL_PERMITIDO:
            duracionIndicador = DURACION_ACCESO_MS;
            digitalWrite(LED_VERDE, HIGH);
            break;
        case SENAL_DENEGADO:
            duracionIndicador = DURACION_ACCESO_MS;
            digitalWrite(LED_ROJO, HIGH);
            break;
        case SENAL_ABRIR:
            duracionIndicador = DURACION_ABRIR_MS;
            digitalWrite(LED_VERDE, HIGH);
            break;
        case SENAL_ALARMA:
            duracionIndicador = DURACION_ALARMA_MS;
            digitalWrite(LED_ROJO, HIGH);
            break;
    }
    inicioIndicador = ahora;
    indicadorActivo = true;
    actualizarIndicador(ahora);
}

void enviarSenal(Senal senal) {
    if (colaSenales == nullptr) {
        return;
    }
    if (xQueueSend(colaSenales, &senal, 0) != pdPASS) {
        Serial.println("Senal omitida: cola llena.");
    }
}

void revisarSenales(unsigned long ahora) {
    Senal senal;
    while (colaSenales != nullptr && xQueueReceive(colaSenales, &senal, 0) == pdTRUE) {
        iniciarSenal(senal, ahora);
    }
}

// ---------- HTTP (tarea de red) ----------

void procesarRespuestaAcceso(int codigo, const String& cuerpo) {
    if (codigo <= 0) {
        Serial.printf("Acceso: error de comunicacion (%d). Sin confirmacion.\n", codigo);
        enviarSenal(SENAL_DENEGADO);
        return;
    }
    if (codigo != HTTP_CODE_OK) {
        Serial.printf("Acceso: error HTTP %d. Sin confirmacion.\n", codigo);
        enviarSenal(SENAL_DENEGADO);
        return;
    }

    JsonDocument respuesta;
    if (deserializeJson(respuesta, cuerpo) || !respuesta["success"].is<bool>()) {
        Serial.println("Acceso: respuesta JSON invalida. Sin confirmacion.");
        enviarSenal(SENAL_DENEGADO);
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
            enviarSenal(SENAL_DENEGADO);
            return;
        }
        enviarSenal(SENAL_PERMITIDO);
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
    enviarSenal(SENAL_DENEGADO);
}

void enviarAcceso(const char* uid) {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("Acceso: sin WiFi. Retira y vuelve a presentar la tarjeta.");
        enviarSenal(SENAL_DENEGADO);
        return;
    }

    WiFiClient cliente;
    HTTPClient http;
    if (!http.begin(cliente, urlApi("esp32/acceso.php"))) {
        Serial.println("Acceso: no se pudo iniciar la solicitud. Revisa SERVER.");
        enviarSenal(SENAL_DENEGADO);
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

bool enviarMedicion() {
    const float temperatura = dht.readTemperature();
    const float humedad = dht.readHumidity();
    if (!isfinite(temperatura) || !isfinite(humedad)) {
        Serial.println("Lectura invalida del sensor DHT. Medicion omitida.");
        return false;
    }

    WiFiClient cliente;
    HTTPClient http;
    if (!http.begin(cliente, urlApi("esp32/medicion.php"))) {
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
            Serial.printf("Medicion registrada: %.1f C, %.1f %% (HTTP 200)\n", temperatura,
                          humedad);
            if (respuesta["alerta"] | false) {
                Serial.println("Medicion: alerta activa (supera el limite).");
                enviarSenal(SENAL_DENEGADO);
            }
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

void consultarComando() {
    if (WiFi.status() != WL_CONNECTED) {
        return;
    }

    // Un solo aviso por caida, para no inundar el monitor cada 3 s.
    static bool errorAvisado = false;

    WiFiClient cliente;
    HTTPClient http;
    if (!http.begin(cliente, urlApi("esp32/comando.php") + "?api_key=" + API_KEY)) {
        if (!errorAvisado) {
            Serial.println("Comando: URL invalida. Revisa SERVER y API_KEY.");
            errorAvisado = true;
        }
        return;
    }

    http.setConnectTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.setTimeout(TIEMPO_ESPERA_HTTP_MS);
    const int codigo = http.GET();

    if (codigo == HTTP_CODE_OK) {
        errorAvisado = false;
        JsonDocument respuesta;
        if (deserializeJson(respuesta, http.getString())) {
            Serial.println("Comando: respuesta JSON invalida.");
        } else if (!respuesta["comando"].isNull()) {
            const char* accion = respuesta["comando"]["accion"] | "";
            if (strcmp(accion, "abrir_acceso") == 0) {
                Serial.println("Comando: abrir acceso.");
                enviarSenal(SENAL_ABRIR);
            } else if (strcmp(accion, "activar_alarma") == 0 || strcmp(accion, "alarma") == 0) {
                Serial.println("Comando: activar alarma.");
                enviarSenal(SENAL_ALARMA);
            } else {
                Serial.printf("Comando: accion desconocida (%s).\n", accion);
            }
        }
    } else if (codigo <= 0) {
        if (!errorAvisado) {
            Serial.printf("Comando: error de red (%d). Revisa que %s responda.\n", codigo,
                          String(SERVER).c_str());
            errorAvisado = true;
        }
    } else {
        errorAvisado = false;
        Serial.printf("Comando: error HTTP %d.\n", codigo);
    }

    http.end();
}

// ---------- RFID (loop) ----------

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
        if (tareaIo != nullptr) {
            xTaskNotify(tareaIo, AVISO_TARJETAS_RECIENTES, eSetBits);
        }
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
        if (tareaIo != nullptr) {
            xTaskNotify(tareaIo, AVISO_HALT, eSetBits);
        }
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
    if (!registrarLectura(trabajo.uid, trabajo.instante)) {
        return;
    }
    if (xQueueSend(colaTrabajos, &trabajo, 0) != pdPASS) {
        if (tareaIo != nullptr) {
            xTaskNotify(tareaIo, AVISO_COLA_ACCESO, eSetBits);
        }
    }
}

// ---------- tarea de red ----------

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
    iniciarWifi();
    Serial.println("Conectando a WiFi.");
    ultimoIntentoWifi = millis();

    for (;;) {
        informarAvisos();

        const unsigned long ahora = millis();
        actualizarWifi(ahora);

        static unsigned long ultimoComando = 0;
        if (WiFi.status() == WL_CONNECTED && ahora - ultimoComando >= INTERVALO_COMANDO_MS) {
            ultimoComando = ahora;
            consultarComando();
        }

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

// ---------- setup / loop ----------

void setup() {
    Serial.begin(115200);
    delay(300);

    pinMode(LED_VERDE, OUTPUT);
    pinMode(LED_ROJO, OUTPUT);
    pinMode(BUZZER, OUTPUT);
    apagarSalidas();

    SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
    lector.PCD_Init();

    Serial.println();
    Serial.println("Aforo IoT - ESP32 (RFID, LEDs, comandos y mediciones)");

    colaTrabajos = xQueueCreate(CAPACIDAD_COLA, sizeof(Trabajo));
    colaSenales = xQueueCreate(CAPACIDAD_SENALES, sizeof(Senal));
    const BaseType_t nucleoIo = portNUM_PROCESSORS > 1 ? 1 - xPortGetCoreID() : tskNO_AFFINITY;
    if (colaTrabajos == nullptr || colaSenales == nullptr
            || xTaskCreatePinnedToCore(tareaEntradaSalida, "aforo_io", PILA_TAREA_IO_BYTES,
                                       nullptr, 1, &tareaIo, nucleoIo) != pdPASS) {
        if (colaTrabajos != nullptr) {
            vQueueDelete(colaTrabajos);
            colaTrabajos = nullptr;
        }
        if (colaSenales != nullptr) {
            vQueueDelete(colaSenales);
            colaSenales = nullptr;
        }
        Serial.println("No se pudo iniciar la cola o la tarea de E/S.");
        return;
    }

    ultimaMedicion = millis();
}

void loop() {
    if (colaTrabajos == nullptr) {
        return;
    }

    const unsigned long ahora = millis();
    revisarSenales(ahora);
    actualizarIndicador(ahora);
    consultarTarjeta();

    if (ahora - ultimaMedicion >= INTERVALO_MEDICION) {
        ultimaMedicion = ahora;
        const Trabajo trabajo = {{}, ahora};
        if (xQueueSend(colaTrabajos, &trabajo, 0) != pdPASS) {
            if (tareaIo != nullptr) {
                xTaskNotify(tareaIo, AVISO_COLA_MEDICION, eSetBits);
            }
        }
    }
}
