# S1-B1 · Montaje en protoboard

## Descripción breve del montaje

El montaje conecta un ESP32 DevKitC V4 a un lector RC522, un sensor DHT22, dos LEDs y un buzzer. Las tarjetas RFID se acercan al lector sin conectarse con cables. [Issue #9](https://github.com/OsvaldoVar/aforo-iot/issues/9)

## Tabla de conexiones

| Componente | Pines del ESP32 | Conexión |
| --- | --- | --- |
| Protoboard | 3.3 V, GND | Riel positivo de 3.3 V y riel de tierra común, respectivamente. |
| RC522 | GPIO 5, 18, 19, 22 y 23 | GPIO 5 → SDA/SS, 18 → SCK, 19 → MISO, 22 → RST y 23 → MOSI. Alimentación a 3.3 V y GND. |
| DHT22 | GPIO 4 | DATA (SDA en Wokwi). Alimentación a 3.3 V y GND. Si el sensor no tiene resistencia integrada, conectar una de 10 kΩ entre DATA y 3.3 V. |
| LED verde | GPIO 25 | Ánodo mediante una resistencia de 220 Ω y cátodo a GND. |
| LED rojo | GPIO 26 | Ánodo mediante una resistencia de 220 Ω y cátodo a GND. |
| Buzzer de Wokwi | GPIO 27 | Terminal positivo a GPIO 27 y negativo a GND. |

Desconecta la alimentación antes de cambiar cables y alimenta el RC522 con 3.3 V. Antes de conectar el buzzer físico, verifica su tensión y corriente para saber si necesita un circuito de control.

## Diagrama de Wokwi

Copia el contenido de [diagram.json](diagram.json) en la pestaña del mismo nombre de un [proyecto ESP32 en Wokwi](https://wokwi.com/projects/new/esp32).

## Estado actual

Por el momento no cuento con los componentes físicos. El montaje real, las pruebas físicas y las fotografías quedan pendientes. El diagrama está preparado para revisión y su ejecución en Wokwi sigue pendiente.
