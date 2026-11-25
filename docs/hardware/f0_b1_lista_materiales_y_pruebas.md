# F0-B1 · Lista de materiales y prueba de componentes

El [Issue #2](https://github.com/OsvaldoVar/aforo-iot/issues/2) requiere una foto del material y comprobar que cada componente funciona.

## Inventario de materiales

| Material | Cantidad | Descripción |
| --- | --- | --- |
| ESP32 | 1 | Placa que controla los componentes. |
| Lector RC522 | 1 | Módulo para leer las tarjetas RFID. |
| Tarjetas RFID | 3 | Compatibles con el RC522 e identificadas como A, B y C. |
| DHT22 | 1 | Sensor de temperatura y humedad. |
| LED verde | 1 | Indicador luminoso verde. |
| LED rojo | 1 | Indicador luminoso rojo. |
| Resistencias de 220 Ω | 2 | Una para cada LED. |
| Buzzer | 1 | Indicador sonoro. |
| Protoboard | 1 | Base para montar las pruebas. |
| Cable USB de datos | 1 | Para conectar el ESP32 a la computadora. |
| Cables Dupont | 1 juego | Para conectar los componentes. |
| Resistencia para el DHT22 | 1, si aplica | Se usa si el sensor no la trae integrada. |
| Módulo de control para el buzzer | 1, si aplica | Se usa si el buzzer requiere un circuito de control adicional. |

## Lista de verificación de pruebas

| Componente | Qué se debe comprobar | Estado |
| --- | --- | --- |
| ESP32 | Arranca y muestra un mensaje en el monitor serie. | OK |
| RC522 | El ESP32 se comunica con el lector y reconoce su versión. | OK |
| Tarjeta RFID A | Se lee por separado dos veces y se obtiene el mismo identificador. | OK |
| Tarjeta RFID B | Se obtiene el mismo identificador en dos lecturas y es distinto del de A. | OK |
| Tarjeta RFID C | Se obtiene el mismo identificador en dos lecturas y es distinto de los de A y B. | OK |
| DHT22 | Entrega lecturas válidas de temperatura y humedad. | Falla: cableado en reparación |
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

### DHT22

**Falla.** El ESP32 no envió ninguna medición a la tabla `mediciones` durante la prueba: el sensor tiene un problema de cableado pendiente de reparación. Hay que revisar DATA en GPIO 4, alimentación a 3.3 V, GND y la resistencia pull-up de 10 kΩ, y repetir la prueba. Por eso el criterio «una medición del ESP32 real aparece en la tabla `mediciones`» todavía no se cumple.

## Estado actual

El ESP32, el lector RC522, los LEDs y el buzzer están verificados. El DHT22 queda pendiente por cableado; cuando se repare se agregará la salida de `SELECT * FROM mediciones ORDER BY id DESC LIMIT 3;` y se actualizará esta tabla.
