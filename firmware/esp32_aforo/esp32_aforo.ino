// Firmware del nodo ESP32 de Aforo IoT.
//
// Integra lectura RFID con antirrebote (#43), mediciones (#44), LEDs y buzzer
// (#45), comandos remotos (#78) y alertas (#79). La red y el servidor salen de
// config.h por defecto y se pueden cambiar por el monitor serie (se guardan en
// NVS), sin reflashear.

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <Preferences.h>
#include <SPI.h>
#include <MFRC522.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>
#include <math.h>
#include <string.h>

#include "ip_valida.h"
#include "config.h"

// ---------- pines ----------
#define SS_PIN 5
#define RST_PIN 22
#define SCK_PIN 18
#define MISO_PIN 19
#define MOSI_PIN 23
#define DHT_PIN 4
// El kit trae un DHT11. Para un DHT22, define DHT_TIPO DHT22 en config.h.
#ifndef DHT_TIPO
#define DHT_TIPO DHT11
#endif
#define LED_VERDE 25
#define LED_ROJO 26
#define BUZZER 27

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
#define MAX_REDES 30
#define ESPERA_ENTRADA_MS 60000
#define PUERTO_POR_DEFECTO 8080

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

Preferences prefs;
DHT dht(DHT_PIN, DHT_TIPO);
MFRC522 lector(SS_PIN, RST_PIN);
LecturaReciente lecturasRecientes[CAPACIDAD_LECTURAS_RECIENTES] = {};
String redes[MAX_REDES];

QueueHandle_t colaTrabajos = nullptr;
QueueHandle_t colaSenales = nullptr;
TaskHandle_t tareaIo = nullptr;

// Solo se tocan desde la tarea de red (o desde loop con la tarea pausada).
String red;
String clave;
String url;
String apiKey;

unsigned long ultimaMedicion = 0;
unsigned long ultimoIntentoWifi = 0;
unsigned long ultimoFalloApi = 0;
bool wifiConectado = false;
bool apiEnPausa = false;

// Pausa de la tarea de red mientras el menu toca WiFi y configuracion.
volatile bool ioPausada = false;
volatile bool ioEnPausa = false;

// Indicador LED/buzzer, solo en loop().
unsigned long inicioIndicador = 0;
unsigned long duracionIndicador = 0;
Senal senalActual = SENAL_PERMITIDO;
bool indicadorActivo = false;
bool buzzerActivo = false;

// ---------- almacenamiento ----------

void cargarConfig() {
    prefs.begin("aforo", true);
    red = prefs.getString("red", WIFI_SSID);
    clave = prefs.getString("clave", WIFI_PASS);
    url = prefs.getString("url", SERVER);
    apiKey = prefs.getString("api_key", API_KEY);
    prefs.end();
}

void guardarConfig() {
    prefs.begin("aforo", false);
    prefs.putString("red", red);
    prefs.putString("clave", clave);
    prefs.putString("url", url);
    prefs.putString("api_key", apiKey);
    prefs.end();
}

void borrarConfig() {
    prefs.begin("aforo", false);
    prefs.clear();
    prefs.end();
    red = WIFI_SSID;
    clave = WIFI_PASS;
    url = SERVER;
    apiKey = API_KEY;
    Serial.println("Configuracion restaurada desde config.h.");
}

bool configCompleta() {
    return red.length() > 0 && url.length() > 0;
}

String urlBase() {
    return url.endsWith("/") ? url : url + "/";
}

String oculto(const String& valor) {
    return valor.length() > 0 ? String("********") : String("(vacia)");
}

// ---------- entrada por monitor serie ----------

bool entradaExpirada = false;

String leerLinea(const char* aviso) {
    Serial.print(aviso);
    entradaExpirada = false;
    String linea;
    const unsigned long inicio = millis();
    while (millis() - inicio < ESPERA_ENTRADA_MS) {
        while (Serial.available() > 0) {
            const char caracter = static_cast<char>(Serial.read());
            if (caracter == '\n') {
                linea.trim();
                Serial.println();
                return linea;
            }
            linea += caracter;
        }
        delay(5);  // cede el CPU; readStringUntil puede bloquear el WDT
    }
    linea.trim();
    Serial.println();
    entradaExpirada = true;
    return linea;
}

int leerEntero(const char* aviso, int minimo, int maximo) {
    for (;;) {
        const String texto = leerLinea(aviso);
        if (entradaExpirada) {
            return minimo;  // sin respuesta: el llamador revisa entradaExpirada y cierra el menú
        }
        const long valor = texto.toInt();
        if (texto.length() > 0 && valor >= minimo && valor <= maximo) {
            return static_cast<int>(valor);
        }
        Serial.printf("Escribe un numero entre %d y %d.\n", minimo, maximo);
    }
}

// ---------- menu ----------

int escanearRedes() {
    Serial.println("Escaneando redes 2.4 GHz...");
    WiFi.mode(WIFI_STA);
    WiFi.disconnect();
    const int total = WiFi.scanNetworks();
    if (total <= 0) {
        Serial.println("No se encontro ninguna red. Acerca el ESP32 al router.");
        WiFi.scanDelete();
        return 0;
    }
    const int limite = total < MAX_REDES ? total : MAX_REDES;
    for (int i = 0; i < limite; ++i) {
        redes[i] = WiFi.SSID(i);
        Serial.printf("%2d) %-30s %4d dBm  %s\n", i + 1, redes[i].c_str(), WiFi.RSSI(i),
                      WiFi.encryptionType(i) == WIFI_AUTH_OPEN ? "abierta" : "protegida");
    }
    WiFi.scanDelete();
    return limite;
}

void configurarRed() {
    const int total = escanearRedes();
    if (total == 0) {
        return;
    }
    const int elegida = leerEntero("Numero de red (0 cancela): ", 0, total);
    if (elegida == 0) {
        return;
    }
    red = redes[elegida - 1];
    Serial.printf("Red elegida: %s\n", red.c_str());
    const String nuevaClave = leerLinea("Contrasena (Enter si la red es abierta): ");
    if (entradaExpirada) {
        // Sin respuesta no se borra la contrasena que ya funcionaba.
        Serial.println("Sin respuesta: se conserva la contrasena anterior.");
    } else {
        clave = nuevaClave;
    }
    guardarConfig();
    Serial.println("Red guardada en la memoria del ESP32.");
}

void configurarServidor() {
    for (;;) {
        const String ip = leerLinea("IP de la computadora (ej. 192.168.1.10): ");
        if (!ipValida(ip.c_str())) {
            Serial.println("IP invalida. Usa el formato 192.168.1.10.");
            continue;
        }
        const String textoPuerto = leerLinea("Puerto (Enter para 8080): ");
        const long puerto = textoPuerto.length() == 0 ? PUERTO_POR_DEFECTO : textoPuerto.toInt();
        if (puerto < 1 || puerto > 65535) {
            Serial.println("Puerto invalido. Usa un numero entre 1 y 65535.");
            return;
        }
        url = "http://" + ip + ":" + String(puerto) + "/api_aforo/";
        guardarConfig();
        Serial.printf("Servidor: %s\n", url.c_str());
        return;
    }
}

void configurarApiKey() {
    const String texto = leerLinea("API key del dispositivo (Enter deja la actual): ");
    if (texto.length() > 0) {
        apiKey = texto;
        guardarConfig();
        Serial.println("API key actualizada.");
    }
}

void verConfig() {
    Serial.printf("Red      : %s\n", red.length() > 0 ? red.c_str() : "(sin definir)");
    Serial.printf("Contrasena: %s\n", oculto(clave).c_str());
    Serial.printf("Servidor : %s\n", url.length() > 0 ? urlBase().c_str() : "(sin definir)");
    Serial.printf("API key  : %s\n", oculto(apiKey).c_str());
    Serial.println("La contrasena y la API key se guardan en texto plano en la NVS del ESP32.");
}

void menuConfiguracion() {
    for (;;) {
        Serial.println();
        Serial.println("=== Configuracion Aforo IoT ===");
        Serial.printf("Red      : %s\n", red.length() > 0 ? red.c_str() : "(sin definir)");
        Serial.printf("Servidor : %s\n", url.length() > 0 ? urlBase().c_str() : "(sin definir)");
        Serial.printf("API key  : %s\n", oculto(apiKey).c_str());
        Serial.println("1) Escanear y elegir red");
        Serial.println("2) Cambiar servidor (IP y puerto)");
        Serial.println("3) Cambiar API key");
        Serial.println("4) Ver configuracion");
        Serial.println("5) Restaurar config.h");
        Serial.println("6) Guardar y conectar");
        Serial.println("0) Cancelar");
        const int opcion = leerEntero("Opcion: ", 0, 6);

        switch (opcion) {
            case 0:
                cargarConfig();
                return;
            case 1:
                configurarRed();
                break;
            case 2:
                configurarServidor();
                break;
            case 3:
                configurarApiKey();
                break;
            case 4:
                verConfig();
                break;
            case 5:
                borrarConfig();
                break;
            case 6:
                if (configCompleta()) {
                    return;
                }
                break;
        }
    }
}

// ---------- WiFi ----------

void iniciarWifi() {
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    if (clave.length() == 0) {
        WiFi.begin(red.c_str());
    } else {
        WiFi.begin(red.c_str(), clave.c_str());
    }
}

bool conectarWifi(unsigned long esperaMs) {
    if (!configCompleta()) {
        return false;
    }
    iniciarWifi();
    Serial.printf("Conectando a \"%s\"", red.c_str());
    const unsigned long inicio = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - inicio < esperaMs) {
        Serial.print('.');
        delay(500);
    }
    Serial.println();
    wifiConectado = WiFi.status() == WL_CONNECTED;
    if (wifiConectado) {
        Serial.printf("Conectado. IP del ESP32: %s\n", WiFi.localIP().toString().c_str());
        Serial.printf("Servidor configurado: %s\n", urlBase().c_str());
    } else {
        Serial.println("No se pudo conectar. Revisa la contrasena y que la red sea de 2.4 GHz.");
    }
    return wifiConectado;
}

void actualizarWifi(unsigned long ahora) {
    if (!configCompleta()) {
        wifiConectado = false;
        return;
    }
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
    if (!http.begin(cliente, urlBase() + "esp32/acceso.php")) {
        Serial.println("Acceso: no se pudo iniciar la solicitud. Revisa la IP del servidor.");
        enviarSenal(SENAL_DENEGADO);
        return;
    }

    http.setConnectTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.setTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.addHeader("Content-Type", "application/json");

    JsonDocument acceso;
    acceso["api_key"] = apiKey;
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
    if (!http.begin(cliente, urlBase() + "esp32/medicion.php")) {
        Serial.println("No se pudo iniciar la solicitud. Revisa la IP del servidor.");
        return false;
    }

    http.setConnectTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.setTimeout(TIEMPO_ESPERA_HTTP_MS);
    http.addHeader("Content-Type", "application/json");

    JsonDocument medicion;
    medicion["api_key"] = apiKey;
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
    if (!http.begin(cliente, urlBase() + "esp32/comando.php?api_key=" + apiKey)) {
        if (!errorAvisado) {
            Serial.println("Comando: URL invalida. Revisa la IP y el puerto.");
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
            Serial.printf("Comando: error de red (%d). El ESP32 no alcanza %s\n", codigo,
                          urlBase().c_str());
            Serial.printf("  ESP32: %s | revisa servidor encendido, puerto y firewall.\n",
                          WiFi.localIP().toString().c_str());
            errorAvisado = true;
        }
    } else {
        errorAvisado = false;
        Serial.printf("Comando: error HTTP %d en %s\n", codigo, urlBase().c_str());
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
    // Con la cola llena no se registra el antirrebote, para que la tarjeta pueda volver a leerse.
    if (uxQueueSpacesAvailable(colaTrabajos) == 0) {
        if (tareaIo != nullptr) {
            xTaskNotify(tareaIo, AVISO_COLA_ACCESO, eSetBits);
        }
        return;
    }
    if (!registrarLectura(trabajo.uid, trabajo.instante)) {
        return;
    }
    xQueueSend(colaTrabajos, &trabajo, 0);
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
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(false);
    if (configCompleta()) {
        Serial.println("Conectando a WiFi.");
        ultimoIntentoWifi = millis();
        conectarWifi(20000);
    } else {
        Serial.println("Sin red configurada. Escribe 'm' y Enter para configurar.");
    }

    for (;;) {
        if (ioPausada) {
            ioEnPausa = true;
            vTaskDelay(pdMS_TO_TICKS(50));
            continue;
        }
        ioEnPausa = false;
        informarAvisos();

        const unsigned long ahora = millis();
        actualizarWifi(ahora);

        static unsigned long ultimoComando = 0;
        if (configCompleta() && WiFi.status() == WL_CONNECTED
                && ahora - ultimoComando >= INTERVALO_COMANDO_MS) {
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

// ---------- menu en caliente ----------

void abrirMenu() {
    apagarSalidas();
    indicadorActivo = false;
    ioPausada = true;

    // Espera a que la tarea de red termine el trabajo en curso (maximo ~6 s por HTTP).
    const unsigned long inicio = millis();
    while (!ioEnPausa && millis() - inicio < 8000) {
        delay(10);
    }
    if (!ioEnPausa) {
        Serial.println("La tarea de red sigue ocupada. Intenta de nuevo en unos segundos.");
        ioPausada = false;
        ultimaMedicion = millis();
        return;
    }

    menuConfiguracion();
    WiFi.disconnect();
    if (configCompleta() && !conectarWifi(20000)) {
        Serial.println("Escribe 'm' y Enter para corregir la red o la IP.");
    }
    wifiConectado = WiFi.status() == WL_CONNECTED;

    // Lo leido mientras el menu estaba abierto ya no sirve: no registrar accesos viejos.
    Senal senalVieja;
    while (colaSenales != nullptr && xQueueReceive(colaSenales, &senalVieja, 0) == pdTRUE) {
    }
    xQueueReset(colaTrabajos);

    ioPausada = false;
    ultimaMedicion = millis();  // evita una rafaga al salir del menu
}

void revisarEntradaSerial() {
    while (Serial.available() > 0) {
        const int tecla = Serial.read();
        if (tecla != 'm' && tecla != 'M') {
            continue;
        }
        // Descarta el Enter que acompana a la 'm' para no ensuciar el menu.
        while (Serial.available() > 0) {
            const int siguiente = Serial.peek();
            if (siguiente == '\r' || siguiente == '\n') {
                Serial.read();
            } else {
                break;
            }
        }
        Serial.println("\nAbriendo menu de configuracion...");
        abrirMenu();
        return;
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

    cargarConfig();
    Serial.println();
    Serial.println("Aforo IoT - ESP32 (RFID, LEDs, comandos y mediciones)");
    Serial.println("Red y servidor: config.h. Escribe 'm' y Enter para cambiarlos.");

    if (!configCompleta()) {
        Serial.println("No hay configuracion guardada.");
        menuConfiguracion();
    }

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

    revisarEntradaSerial();
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
    vTaskDelay(pdMS_TO_TICKS(10));  // cede el núcleo 1 entre sondeos del RC522
}
