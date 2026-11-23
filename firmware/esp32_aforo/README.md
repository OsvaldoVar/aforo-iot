# S1-B4 · Mediciones del ESP32 por WiFi

1. Con el ESP32 apagado, conecta el DHT22 a 3.3 V y GND y lleva DATA a GPIO 4. Si el sensor carece de resistencia integrada, conecta una de 10 kΩ entre DATA y 3.3 V.
2. En Arduino IDE, instala el soporte para ESP32, **ArduinoJson 7**, **DHT sensor library** y **Adafruit Unified Sensor**. WiFi y HTTPClient vienen con el soporte para ESP32.
3. Copia `config.example.h` a `config.h`. Configura la red de 2.4 GHz y usa la clave de prueba indicada en el Issue. En `SERVER`, indica la dirección de la computadora que ejecuta el backend, con el puerto 8080 y el sufijo `/api_aforo/`. El ESP32 debe poder alcanzar esa computadora desde la misma red. Mantén `config.h` local. [Issue #12](https://github.com/OsvaldoVar/aforo-iot/issues/12)
4. Carga `esp32_aforo.ino` y abre el monitor serie a **115200 baudios**. El firmware intenta registrar una medición cada 15 segundos y confirma el registro cuando recibe HTTP 200 y `success: true`. Si no hay WiFi o la lectura es inválida, omite ese envío y continúa en el siguiente intervalo.

Para la prueba de integración, comprueba que aparezca una fila en `mediciones` después del mensaje de confirmación. Las pruebas con el equipo físico corresponden al [Issue #31](https://github.com/OsvaldoVar/aforo-iot/issues/31).
