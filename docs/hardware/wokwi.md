# Simulación en Wokwi — Aforo IoT

Esta guía explica cómo cargar el circuito de [diagram.json](diagram.json) y el firmware de [`firmware/esp32_aforo/`](../../firmware/esp32_aforo/) en el simulador en línea **Wokwi** para probar el comportamiento local del ESP32 sin hardware físico.

[Proyecto Aforo IoT en Wokwi](https://wokwi.com/projects/481686965909601281)

## Nota sobre el sensor DHT en Wokwi

El archivo [diagram.json](diagram.json) define las 22 conexiones del circuito y utiliza la pieza `wokwi-dht22`, ya que Wokwi no cuenta con un componente DHT11.

En el hardware físico del kit el firmware usa `DHT11` por defecto; para que las lecturas funcionen dentro de Wokwi es necesario definir `#define DHT_TIPO DHT22` en `config.h`.

## Pasos para preparar el proyecto

1. Abre un proyecto nuevo de ESP32 en Wokwi: [https://wokwi.com/projects/new/esp32](https://wokwi.com/projects/new/esp32).
2. En la pestaña **`diagram.json`**, reemplaza todo el contenido por el de [docs/hardware/diagram.json](diagram.json).
3. En la pestaña **`sketch.ino`**, pega el contenido de [`firmware/esp32_aforo/esp32_aforo.ino`](../../firmware/esp32_aforo/esp32_aforo.ino).
4. Crea la pestaña **`ip_valida.h`** (botón de nuevo archivo en Wokwi) y pega el contenido de [`firmware/esp32_aforo/ip_valida.h`](../../firmware/esp32_aforo/ip_valida.h).
5. Crea la pestaña **`libraries.txt`** con las librerías requeridas:

```txt
MFRC522
DHT sensor library
Adafruit Unified Sensor
ArduinoJson
```

6. Crea la pestaña **`config.h`** usando la red abierta del simulador (`Wokwi-GUEST`) y activando `DHT22`:

```c
#pragma once

#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASS ""
#define SERVER "http://127.0.0.1:8080/api_aforo/"
#define API_KEY "demo-esp32-key-0001"

// Necesario en Wokwi porque diagram.json usa wokwi-dht22 (no hay DHT11 en Wokwi).
#define DHT_TIPO DHT22
```

7. Inicia la simulación y abre el Monitor Serie a **115200 baudios**.

## Alcance de la simulación y errores HTTP esperados

El simulador en la nube de Wokwi no puede alcanzar el servidor PHP local (`php -S`) que corre en tu computadora. Por esta razón, los mensajes de error de red o error HTTP en el monitor serie son **esperados** durante la simulación (tanto al enviar accesos como al consultar comandos o registrar mediciones).

### Qué sí se puede validar en Wokwi

- **Lectura de UID RFID:** al interactuar con la pieza `board-mfrc522` del diagrama, el firmware lee la tarjeta y procesa su UID en hexadecimal mayúsculas.
- **Antirrebote RFID:** presentar la misma tarjeta de forma repetida dentro de la ventana de 3 s (`ANTIRREBOTE_RFID_MS`) no genera envíos duplicados.
- **LEDs y buzzer:** ante una lectura sin respuesta válida del servidor, el firmware activa el LED rojo durante 2 s junto con dos pitidos de 100 ms en el buzzer, confirmando el cableado y la señalización no bloqueante.
- **Lecturas del sensor DHT:** al variar los deslizadores de temperatura y humedad en `wokwi-dht22`, el firmware lee valores válidos cada 15 s (sin reportar lecturas `NaN` ni bloquear el lector RFID).
- **Menú de configuración por serie:** escribir `m` y Enter en el monitor serie permite recorrer las opciones de configuración almacenadas en NVS.

## Pruebas del Issue #100

| Prueba | Estado |
| --- | --- |
| Conexión a `Wokwi-GUEST` e intento de envío de mediciones cada 15 s | Pendiente |
| Lectura del UID y activación del LED rojo y el buzzer ante un fallo de comunicación | Pendiente |
| Antirrebote al volver a presentar la misma tarjeta antes de 3 s | Pendiente |
| Lectura del nuevo valor al cambiar la temperatura del DHT22 simulado | Pendiente |

## Referencias

- Esquema y tabla de pines: [conexiones.md](conexiones.md)
- Archivo de circuito para Wokwi: [diagram.json](diagram.json)
- Código fuente del firmware: [`firmware/esp32_aforo/esp32_aforo.ino`](../../firmware/esp32_aforo/esp32_aforo.ino)
