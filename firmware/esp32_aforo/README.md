# Firmware del ESP32 · Aforo IoT

Nodo IoT del proyecto: lee tarjetas RFID, enciende LEDs y buzzer, mide temperatura y humedad, atiende comandos remotos y reporta las alertas a la API.

## Qué incluye

- Lectura del RC522 con UID en hexadecimal mayúsculas y antirrebote de 3 s por UID (#43).
- Medición del sensor DHT cada 15 s, sin bloquear la lectura de tarjetas (#44).
- Señalización local con LED verde, LED rojo y buzzer, sin `delay()` en `loop()` (#45).
- Consulta de comandos cada 3 s: `abrir_acceso` y `activar_alarma`/`alarma` (#78).
- LED rojo y pitidos cuando la API responde `aforo_lleno` o la medición trae `alerta: true` (#79).
- Cola FIFO y tarea FreeRTOS de red: el sondeo RFID no se bloquea con las peticiones HTTP.
- Reconexión de WiFi cada 10 s y pausa de 30 s tras fallos temporales de la API.

## Conexiones

| Componente | Pines del ESP32 |
| --- | --- |
| RC522 | SDA/SS 5, SCK 18, MISO 19, RST 22, MOSI 23; 3.3 V y GND |
| DHT11 | DATA 4; 3.3 V y GND; pull-up de 10 kΩ si el módulo no lo trae |
| LED verde | GPIO 25 con resistencia de 220 Ω |
| LED rojo | GPIO 26 con resistencia de 220 Ω |
| Buzzer | GPIO 27 y GND |

El detalle completo está en [docs/instalacion.md](../../docs/instalacion.md#3-firmware-esp32).

## Configuración

1. Copia la plantilla:
   ```bash
   cp firmware/esp32_aforo/config.example.h firmware/esp32_aforo/config.h
   ```
2. Edita `config.h` con tu red de 2.4 GHz, la URL de la API (con `/api_aforo/` al final) y la `API_KEY` del dispositivo.
3. El kit trae un **DHT11**; el firmware lo usa por defecto. Si reemplazas el sensor por un DHT22, agrega `#define DHT_TIPO DHT22` en `config.h`.

`config.h` está en `.gitignore`: no se sube al repositorio.

## Compilar y cargar

Con Arduino IDE 2: abre `esp32_aforo.ino`, placa **ESP32 Dev Module**, instala **MFRC522**, **DHT sensor library**, **Adafruit Unified Sensor** y **ArduinoJson 7**, y sube el sketch.

Con `arduino-cli`:

```bash
arduino-cli compile --fqbn esp32:esp32:esp32 firmware/esp32_aforo
arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 firmware/esp32_aforo
arduino-cli monitor -p /dev/ttyUSB0 -c baudrate=115200
```

## Comportamiento

| Evento | LED | Buzzer |
| --- | --- | --- |
| Entrada o salida registrada | Verde 2 s | Silencio |
| Tarjeta desconocida, `aforo_lleno`, alerta o error HTTP/red | Rojo 2 s | 2 pulsos de 100 ms |
| Comando `abrir_acceso` | Verde 3 s | Silencio |
| Comando `activar_alarma` o `alarma` | Rojo 3 s | Continuo 3 s |

Mensajes esperados en el monitor serie:

```
WiFi conectado.
Acceso: entrada registrada.
Acceso: denegado por aforo lleno.
Medicion registrada: 24.5 C, 42.0 % (HTTP 200)
Comando: abrir acceso.
Comando: activar alarma.
```

## Pruebas

El firmware se validó con el hardware real en las pruebas de [hardware](../../docs/pruebas/pruebas-hardware.md) y de [#31](https://github.com/OsvaldoVar/aforo-iot/issues/31): 20 lecturas consecutivas, antirrebote, tarjeta desconocida, aforo lleno, API caída, sensor desconectado, apertura remota y alarma.
