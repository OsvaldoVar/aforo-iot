# S1-B3 · Prueba del lector RC522

Este sketch del [Issue #11](https://github.com/OsvaldoVar/aforo-iot/issues/11) muestra el UID de cada tarjeta RFID compatible en hexadecimal, con mayúsculas y dos dígitos por byte.

1. Con el ESP32 apagado, conecta el RC522 a 3.3 V y GND. Conecta SDA/SS a GPIO 5, RST a GPIO 22, SCK a GPIO 18, MISO a GPIO 19 y MOSI a GPIO 23. [Montaje del Issue #9](https://github.com/OsvaldoVar/aforo-iot/blob/586bfb8bccd6d35f91c0b984da8d3f5180bbed4e/docs/hardware/s1_b1_montaje_protoboard.md)
2. En Arduino IDE, instala el soporte para ESP32 y la biblioteca **MFRC522**. Abre `prueba_rc522.ino`, selecciona la placa y el puerto, y carga el sketch.
3. Abre el monitor serie a **115200 baudios**. Acerca una tarjeta, retírala y presenta la siguiente. Entre lecturas, el monitor queda a la espera.

La verificación física de que las tres tarjetas tienen UID distintos y estables se realizará en el [Issue #31](https://github.com/OsvaldoVar/aforo-iot/issues/31).
