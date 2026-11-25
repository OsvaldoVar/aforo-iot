# F0-B1 · Lista de materiales y prueba de componentes

El [Issue #2](https://github.com/OsvaldoVar/aforo-iot/issues/2) requiere una foto del material y comprobar que cada componente funciona.

## Inventario de materiales

| Material | Cantidad | Descripción |
| --- | --- | --- |
| ESP32 | 1 | Placa que controla los componentes. |
| Lector RC522 | 1 | Módulo para leer las tarjetas RFID. |
| Tarjetas RFID | 3 | Compatibles con el RC522 e identificadas como A, B y C. |
| DHT22 | 1 | Sensor de temperatura y humedad. El módulo entregado en el kit es un DHT11. |
| LED verde | 1 | Indicador luminoso verde. |
| LED rojo | 1 | Indicador luminoso rojo. |
| Resistencias de 220 Ω | 2 | Una para cada LED. |
| Buzzer | 1 | Indicador sonoro. |
| Protoboard | 1 | Base para montar las pruebas. |
| Cable USB de datos | 1 | Para conectar el ESP32 a la computadora. |
| Cables Dupont | 1 juego | Para conectar los componentes. |
| Resistencia para el sensor DHT | 1, si aplica | Se usa si el sensor no la trae integrada. |
| Módulo de control para el buzzer | 1, si aplica | Se usa si el buzzer requiere un circuito de control adicional. |

## Lista de verificación de pruebas

| Componente | Qué se debe comprobar | Estado |
| --- | --- | --- |
| ESP32 | Arranca y muestra un mensaje en el monitor serie. | OK |
| RC522 | El ESP32 se comunica con el lector y reconoce su versión. | OK |
| Tarjeta RFID A | Se lee por separado dos veces y se obtiene el mismo identificador. | OK |
| Tarjeta RFID B | Se obtiene el mismo identificador en dos lecturas y es distinto del de A. | OK |
| Tarjeta RFID C | Se obtiene el mismo identificador en dos lecturas y es distinto de los de A y B. | OK |
| DHT11 (sensor del kit) | Entrega lecturas válidas de temperatura y humedad. | OK |
| LED verde | Enciende y apaga al activarlo por separado. | OK |
| LED rojo | Enciende y apaga al activarlo por separado. | OK |
| Buzzer | Suena al activarlo y deja de sonar al desactivarlo. | OK |

## Evidencias

Pruebas realizadas el 25 de noviembre de 2026 con el ESP32, la app Android y la API en la red local.

### Fotos

- [Material sobre la mesa](fotos/material.jpg)
- [Montaje terminado](fotos/montaje.jpg)

### Lecturas RFID

El lector RC522 entregó tres identificadores distintos. Cada uno se presentó más de una vez y conservó su valor, lo que confirma el antirrebote y la estabilidad de la lectura.

| UID | Lecturas registradas |
| --- | --- |
| `12121212` | 09:11:04 y 09:11:08 |
| `19281038` | 09:11:12 y 09:18:23 |
| `9749A07F` | 09:11:17, 09:11:23 y 09:14:07 |

Las tres UID anteriores quedaron en `tarjetas_pendientes` por no estar en el padrón. Para probar el acceso reconocido se emularon por NFC las UID de las personas del seed:

| Persona | UID | Hora | Resultado |
| --- | --- | --- | --- |
| Ana Martinez | `A1B2C3D4` | 09:15:06 | entrada registrada |
| Carlos Lopez | `B2C3D4E5` | 09:15:20 | salida registrada |
| Sofia Ramirez | `C3D4E5F6` | 09:15:24 | entrada registrada |

El LED verde se encendió con las UID registradas; el LED rojo y los pitidos, con las desconocidas. El buzzer sonó al activarse y se detuvo al desactivarse.

### DHT11

El módulo instalado es un **DHT11**, aunque el BOM del proyecto indicaba DHT22. Con el driver `DHT11` entrega lecturas válidas y el ESP32 las registra cada 15 s sin bloquear el resto del ciclo:

```sql
SELECT id, temperatura, humedad, fecha FROM mediciones ORDER BY id DESC LIMIT 3;
```

| id | temperatura | humedad | fecha |
| --- | --- | --- | --- |
| 16 | 24.50 | 42.00 | 2026-11-25 09:33:10 |
| 15 | 24.50 | 42.00 | 2026-11-25 09:32:54 |
| 14 | 24.50 | 42.00 | 2026-11-25 09:32:20 |

Con `DHT22` la librería devolvía `NaN` y no se registraba ninguna medición. El firmware del repositorio todavía declara `DHT dht(DHT_PIN, DHT22)`; debe cambiarse a `DHT11` para este hardware o sustituirse el sensor por un DHT22 real.

## Estado actual

El ESP32, el lector RC522, los LEDs, el buzzer y el sensor DHT11 están verificados, y hay mediciones reales del dispositivo en la base de datos. Queda pendiente alinear el firmware del repositorio al DHT11 del kit.
