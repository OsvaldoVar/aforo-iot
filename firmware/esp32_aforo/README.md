# S1-B4 · Mediciones del ESP32 por WiFi

1. Con el ESP32 apagado, conecta el DHT11 del kit (o un DHT22, con `#define DHT_TIPO DHT22` en `config.h`) a 3.3 V y GND y lleva DATA a GPIO 4. Si el sensor carece de resistencia integrada, conecta una de 10 kΩ entre DATA y 3.3 V.
2. En Arduino IDE, instala el soporte para ESP32, **ArduinoJson 7**, **DHT sensor library** y **Adafruit Unified Sensor**. WiFi y HTTPClient vienen con el soporte para ESP32.
3. Copia `config.example.h` a `config.h`. Configura la red de 2.4 GHz y usa la clave de prueba indicada en el Issue. En `SERVER`, indica la dirección de la computadora que ejecuta el backend, con el puerto 8080 y el sufijo `/api_aforo/`. El ESP32 debe poder alcanzar esa computadora desde la misma red. Mantén `config.h` local. [Issue #12](https://github.com/OsvaldoVar/aforo-iot/issues/12)
4. Carga `esp32_aforo.ino` y abre el monitor serie a **115200 baudios**. En condiciones normales, el firmware intenta registrar una medición cada 15 segundos y confirma el registro cuando recibe HTTP 200 y `success: true`. Si no hay WiFi o la lectura es inválida, omite ese envío y continúa en el siguiente intervalo.

Para la prueba de integración, comprueba que aparezca una fila en `mediciones` después del mensaje de confirmación. Las pruebas con el equipo físico corresponden al [Issue #31](https://github.com/OsvaldoVar/aforo-iot/issues/31).

## Reconexión y recuperación

- Si se pierde el WiFi, el ESP32 intenta reconectarse cada 10 segundos. Reanuda el envío cuando vuelve la conexión y llega el siguiente intervalo de medición.
- Ante un error de red, HTTP 408, 429 o 5xx, o una respuesta HTTP 200 con JSON inválido, espera al menos 30 segundos. En el siguiente intervalo disponible envía una lectura nueva. El POST anterior pudo haberse guardado, por eso el firmware no lo reenvía.
- HTTP 400 indica datos rechazados y HTTP 401 indica que debe revisarse `API_KEY`. Se registran en el monitor serie y no activan la pausa por fallos temporales.

Para validar la recuperación, observa el monitor serie a 115200 baudios. Con el ESP32 encendido, apaga y enciende el router de prueba y comprueba que se reconecte sin reiniciar. Después, con WiFi disponible, detén y vuelve a iniciar la API. Verifica que los intentos se espacien durante la interrupción, que se reanuden las confirmaciones HTTP 200 y que la tabla `mediciones` reciba nuevas filas al recuperarse el servicio.
