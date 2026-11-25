# 05 · Protocolo IoT

## Resumen

El ESP32 se comunica con la API PHP por HTTP sobre WiFi de 2.4 GHz. Las peticiones con cuerpo JSON usan `Content-Type: application/json` y los mensajes se codifican en UTF-8. El ESP32 se autentica con la `api_key` de un dispositivo activo y Android usa `X-Token` para consultar y operar la API. PHP es el único componente de este flujo que accede a MariaDB.

En `develop` (integrado mediante el PR #110), los tres endpoints del ESP32 y el firmware completo de [`firmware/esp32_aforo/esp32_aforo.ino`](../../firmware/esp32_aforo/esp32_aforo.ino) están operativos: lectura RFID con antirrebote, señalización local con LEDs y buzzer, envío de mediciones ambientales, consulta de comandos remotos y alertas por aforo lleno o límite ambiental. Una tarea dedicada de FreeRTOS (`tareaEntradaSalida`) y colas FIFO separan el sondeo RFID de la red y del sensor DHT. Además, el firmware incluye un menú de configuración por monitor serie (`m` + Enter) para cambiar red WiFi, servidor y API key y guardarlos en la NVS del ESP32 (namespace `aforo`), usando `config.h` como valores iniciales.

## Endpoints del ESP32

Las rutas son relativas a `/api_aforo/`. Las respuestas JSON incluyen `success` y `mensaje`, junto con los campos específicos en el nivel raíz.

| Endpoint | Método | Cuándo se llama | Cuerpo enviado | Respuesta | Códigos HTTP |
| --- | --- | --- | --- | --- | --- |
| `esp32/acceso.php` | POST | Al aceptar una lectura RFID. | JSON con `api_key` y `uid` hexadecimal. El servidor recorta el UID y lo convierte a mayúsculas. | Con `success: true`, devuelve `tipo` (`entrada` o `salida`), `nombre` y `dentro`. Con `success: false`, el campo `error` distingue `tarjeta_no_registrada` de `aforo_lleno`. | `200` para registro o rechazo de negocio, `400` por JSON inválido o UID vacío, `401` por autenticación, `405` por método y `500` por fallo interno. |
| `esp32/medicion.php` | POST | Cada 15 s en condiciones normales. | JSON con `api_key`, `temperatura` en °C y `humedad` en % HR. Ambos valores deben ser numéricos y finitos. | `medicion_id`, `dispositivo_id`, temperatura, humedad y `alerta` booleano, que indica si se superó algún límite. | `200` para registro, `400` por JSON o datos inválidos, `401` por autenticación, `405` por método y `500` por fallo interno. |
| `esp32/comando.php` | GET | Cada 3 s. | Sin cuerpo. Envía `api_key` en la URL. | `comando: null` si la cola está vacía. Si hay un comando en cola, devuelve `id`, `dispositivo_id`, `accion`, `estado`, `fecha_creacion` y `fecha_ejecucion` dentro de `comando`. | `200` con comando o cola vacía, `401` por autenticación, `405` por método y `500` por fallo interno. |

Los POST también admiten `X-Api-Key` si el campo del cuerpo está vacío. El GET de comandos busca la clave, en orden, en la URL, en `X-Api-Key` y en un eventual cuerpo JSON.

## Ejemplos JSON

Las respuestas se capturaron en la etapa 1 contra PHP y MariaDB aislados, con peticiones de la colección Postman y datos de prueba. La clave de los ejemplos pertenece al dispositivo de demostración del proyecto.

### Acceso

Petición de prueba

```http
POST /api_aforo/esp32/acceso.php
Content-Type: application/json

{"api_key":"demo-esp32-key-0001","uid":"A1B2C3D4"}
```

Respuesta observada, HTTP 200

```json
{"success":true,"mensaje":"Entrada registrada","tipo":"entrada","nombre":"Ana Martinez","dentro":2}
```

### Medición

Petición de prueba

```http
POST /api_aforo/esp32/medicion.php
Content-Type: application/json

{"api_key":"demo-esp32-key-0001","temperatura":24.5,"humedad":55.2}
```

Respuesta observada, HTTP 200

```json
{"success":true,"mensaje":"Medición registrada","medicion_id":7,"dispositivo_id":1,"temperatura":24.5,"humedad":55.2,"alerta":false}
```

### Comando

Petición de prueba

```http
GET /api_aforo/esp32/comando.php?api_key=demo-esp32-key-0001
```

Respuesta observada, HTTP 200

```json
{"success":true,"mensaje":"No hay comandos pendientes","comando":null}
```

Esta ejecución observó la cola vacía. La entrega de un comando y la consulta siguiente sin pendientes están registradas en los casos API-76 y API-77 del [informe de pruebas de la API](../pruebas/informe-pruebas-api.md).

## Tiempos

| Operación | Intervalo o espera | Constante / resultado medido |
| --- | --- | --- |
| Medición ambiental | 15 s | `INTERVALO_MEDICION` (`15000UL`). Se programa con `millis()` en condiciones normales; en [pruebas de hardware](../pruebas/pruebas-hardware.md) se validó el registro continuo cada 15 s. El kit usa `DHT11` por defecto; `DHT22` es configurable mediante `DHT_TIPO`. |
| Consulta de comandos | 3 s | `INTERVALO_COMANDO_MS` (`3000UL`). En [pruebas de hardware](../pruebas/pruebas-hardware.md), `abrir_acceso` se ejecutó en 0 s (LED verde durante `DURACION_ABRIR_MS`, 3 s) y `activar_alarma` en 2 s (LED rojo y buzzer durante `DURACION_ALARMA_MS`, 3 s); rango medido de 0 a 2 s. |
| Antirrebote RFID | 3 s por UID | `ANTIRREBOTE_RFID_MS` (`3000UL`). Validado manteniendo una tarjeta 10 s sobre el lector (1 solo registro). Dejar una tarjeta en el campo no equivale a presentarla de nuevo. |
| Señalización de acceso | 2 s | `DURACION_ACCESO_MS` (`2000UL`). En rechazos o alertas se emiten 2 pitidos de `DURACION_PITIDO_MS` (`100UL`) a `FRECUENCIA_BUZZER_HZ` (`2000` Hz). |
| HTTP | 3 s para conexión y 3 s para lectura | `TIEMPO_ESPERA_HTTP_MS` (`3000`). Configurados por separado en `HTTPClient` y ejecutados en la tarea FreeRTOS `tareaEntradaSalida` para no bloquear `loop()`. |
| Reconexión WiFi | 10 s entre intentos | `INTERVALO_RECONEXION` (`10000UL`). Se programa con `millis()` mientras la conexión está caída. |
| Recuperación del envío de mediciones (API caída) | Al menos 30 s tras un fallo temporal | `INTERVALO_REINTENTO_API` (`30000UL`). Al llegar un intervalo disponible, se intenta enviar una muestra nueva. En [pruebas de hardware](../pruebas/pruebas-hardware.md), tras detener la API 30 s, el ESP32 reanudó automáticamente las mediciones y continuó cada 15 s. |
| Refresco del Dashboard | Aproximadamente 5 s | Android consulta la API con `X-Token`. |

## Manejo de errores

- **HTTP 400 y 401.** El firmware de mediciones registra en el monitor serie que los datos fueron rechazados o que debe revisarse la clave. Estos códigos no activan la pausa de los fallos temporales.
- **HTTP 5xx, 408 y 429, errores de red o JSON inválido en HTTP 200.** El envío de mediciones entra en pausa y luego toma una muestra nueva. El POST anterior pudo haber quedado registrado.
- **Sin WiFi o con una lectura no finita.** Se omite la medición. El firmware mantiene los intentos de reconexión aunque el envío esté en pausa.
- **Acceso.** Solo se confirma una entrada o salida si la respuesta HTTP 200 contiene `success: true` como booleano y un `tipo` válido. Los rechazos de negocio, los errores y las respuestas inválidas se registran por separado y activan el LED rojo con dos pitidos. Un POST de acceso con resultado incierto no se reenvía automáticamente.

## Diagrama de secuencia

El diagrama muestra el flujo completo de comunicación entre la tarjeta RFID, el ESP32, la API PHP, MariaDB y la aplicación Android.

```mermaid
sequenceDiagram
    participant T as Tarjeta
    participant E as ESP32
    participant A as API PHP
    participant B as MariaDB
    participant M as App Android

    T->>E: Lectura RFID (UID)
    E->>A: POST esp32/acceso.php {api_key, uid}
    A->>B: Valida tarjeta y aforo, registra el movimiento si procede
    B-->>A: Resultado de la operación
    A-->>E: {success, tipo o error}
    E->>E: LED verde o LED rojo con buzzer

    E->>A: POST esp32/medicion.php {api_key, temperatura, humedad} cada 15 s
    A->>B: Registra medición y evalúa límites
    B-->>A: Resultado de la operación
    A-->>E: {success, medicion_id, alerta}
    opt alerta es true
        E->>E: LED rojo 2 s y 2 pitidos
    end

    M->>A: GET app/estado.php (cada 5 s, X-Token)
    A->>B: Consulta aforo y última medición
    B-->>A: Estado y medición
    A-->>M: Personas dentro, temperatura y humedad

    M->>A: POST app/comando.php {dispositivo_id, accion} con X-Token
    A->>B: Guarda el comando en cola
    A-->>M: HTTP 201, comando encolado
    E->>A: GET esp32/comando.php?api_key=... cada 3 s
    A->>B: Selecciona el comando en cola más antiguo y lo marca ejecutado
    B-->>A: Comando o cola vacía
    A-->>E: {comando: objeto o null}
    opt Se recibe una acción
        E->>E: Activa LED verde 3 s o buzzer según la acción
    end
```

En el flujo de comandos, el estado `ejecutado` confirma que la API entregó el comando. La actuación física carece de un acuse separado del ESP32.
