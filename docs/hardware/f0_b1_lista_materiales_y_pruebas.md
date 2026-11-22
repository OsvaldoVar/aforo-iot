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
| ESP32 | Arranca y muestra un mensaje en el monitor serie. | Pendiente |
| RC522 | El ESP32 se comunica con el lector y reconoce su versión. | Pendiente |
| Tarjeta RFID A | Se lee por separado dos veces y se obtiene el mismo identificador. | Pendiente |
| Tarjeta RFID B | Se obtiene el mismo identificador en dos lecturas y es distinto del de A. | Pendiente |
| Tarjeta RFID C | Se obtiene el mismo identificador en dos lecturas y es distinto de los de A y B. | Pendiente |
| DHT22 | Entrega lecturas válidas de temperatura y humedad. | Pendiente |
| LED verde | Enciende y apaga al activarlo por separado. | Pendiente |
| LED rojo | Enciende y apaga al activarlo por separado. | Pendiente |
| Buzzer | Suena al activarlo y deja de sonar al desactivarlo. | Pendiente |

## Estado actual

Por el momento no cuento con pruebas físicas realizadas ni con fotografías del material. Estas evidencias quedan pendientes y se agregarán cuando estén disponibles.
