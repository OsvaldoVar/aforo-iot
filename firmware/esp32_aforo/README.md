# S1-B4 · Mediciones del ESP32 por WiFi

1. Con el ESP32 apagado, conecta el DHT22 a 3.3 V y GND y lleva DATA a GPIO 4. Si el sensor carece de resistencia integrada, conecta una de 10 kΩ entre DATA y 3.3 V.
2. En Arduino IDE, instala el soporte para ESP32, **ArduinoJson 7**, **DHT sensor library** y **Adafruit Unified Sensor**. WiFi y HTTPClient vienen con el soporte para ESP32.
3. Copia `config.example.h` a `config.h`. Configura la red de 2.4 GHz y usa la clave de prueba indicada en el Issue. En `SERVER`, indica la dirección de la computadora que ejecuta el backend, con el puerto 8080 y el sufijo `/api_aforo/`. El ESP32 debe poder alcanzar esa computadora desde la misma red. Mantén `config.h` local. [Issue #12](https://github.com/OsvaldoVar/aforo-iot/issues/12)
4. Carga `esp32_aforo.ino` y abre el monitor serie a **115200 baudios**. En condiciones normales, el firmware intenta registrar una medición cada 15 segundos y confirma el registro cuando recibe HTTP 200 y `success: true`. Si no hay WiFi o la lectura es inválida, omite ese envío y continúa en el siguiente intervalo.

Para la prueba de integración, comprueba que aparezca una fila en `mediciones` después del mensaje de confirmación. Las pruebas con el equipo físico corresponden al [Issue #31](https://github.com/OsvaldoVar/aforo-iot/issues/31).

## Reconexión y recuperación

- Si se pierde el WiFi, el ESP32 intenta reconectarse cada 10 segundos. Reanuda el envío cuando vuelve la conexión y llega el siguiente intervalo de medición.
- Ante un error de red, HTTP 408, 429 o 5xx, o una respuesta HTTP 200 con JSON inválido, espera al menos 30 segundos. En el siguiente intervalo disponible envía una lectura nueva. El POST anterior pudo haberse guardado, por eso el firmware no lo reenvía.
- HTTP 400 indica datos rechazados y HTTP 401 indica que debe revisarse `API_KEY`. Se registran en el monitor serie y no activan la pausa por fallos temporales.

Para validar la recuperación, observa el monitor serie a 115200 baudios. Con el ESP32 encendido, apaga y enciende el router de prueba y comprueba que se reconecte sin reiniciar. Después, con WiFi disponible, detén y vuelve a iniciar la API. Verifica que los intentos se espacien durante la interrupción, que se reanuden las confirmaciones HTTP 200 y que la tabla `mediciones` reciba nuevas filas al recuperarse el servicio.

## Lectura RFID

Instala **MFRC522** además de las bibliotecas de medición. **SPI** viene con el soporte para ESP32. Con el equipo apagado, conecta el RC522 a 3.3 V y GND. Conecta SDA/SS a GPIO 5, RST a 22, SCK a 18, MISO a 19 y MOSI a 23. [Montaje en protoboard](../../docs/hardware/s1_b1_montaje_protoboard.md)

El lector se consulta en cada vuelta del ciclo. Envía el UID completo en hexadecimal, en mayúsculas y sin separadores, por POST a `esp32/acceso.php` con `api_key` y `uid`. El monitor serie distingue entrada, salida, tarjeta desconocida, aforo lleno y errores de acceso sin mostrar la clave. Solo se confirma una entrada o salida con HTTP 200, `success: true` booleano y `tipo` válido. La función `procesarRespuestaAcceso(codigo, cuerpo)` es el punto para conectar la señalización posteriormente.

Después de leer una tarjeta, el lector la deja en reposo hasta retirarla. El firmware descarta una nueva presentación del mismo UID durante 3 s, aunque se haya leído otra tarjeta entre medias. Conserva hasta 16 UID recientes y, si todos siguen en su intervalo, omite nuevas lecturas hasta que haya espacio. Una lectura sin WiFi o con respuesta incierta requiere retirar y volver a presentar la tarjeta. El POST de acceso no se reintenta automáticamente ni cambia la pausa de las mediciones.

## Sondeo RFID y tarea de E/S

`loop()` consulta el RC522, aplica el antirrebote y programa una medición cada 15 s con `millis()`. Una tarea FreeRTOS separada lee el DHT22, gestiona la reconexión WiFi y ejecuta los POST. En el ESP32 de dos núcleos, la tarea corre en el núcleo distinto al de `loop()`.

La cola FIFO admite **8 trabajos en espera**, además del que se está procesando. Los accesos se envían en el orden de captura y cada trabajo se procesa una sola vez. Si la cola se llena, se omite el trabajo nuevo y se informa por serie cuando la tarea de E/S puede atender el aviso. Los avisos repetidos del mismo tipo se agrupan. Para un acceso omitido, retira y vuelve a presentar la tarjeta tras el antirrebote. Si no hay WiFi al procesar un acceso, se informa y se omite el envío sin reintento automático.

Las solicitudes HTTP siguen siendo síncronas dentro de la tarea y conservan los tiempos de espera de 3 s para conexión y lectura. Una respuesta lenta puede retrasar la lectura DHT22 o el envío de trabajos en cola, mientras el sondeo RFID continúa. Se omiten con aviso las mediciones que pasan 15 s en cola para evitar una ráfaga de muestras atrasadas.

Para la prueba física, abre el monitor serie a 115200 baudios. Mantén una tarjeta registrada en el lector más de 3 s y confirma que aparece un solo registro. Retírala y vuelve a presentarla tras el intervalo para comprobar la alternancia entrada/salida. Prueba también dos tarjetas distintas seguidas, una desconocida y una lectura con la red desconectada. Confirma que las mediciones continúan al restablecer la red.

Para validar la separación de tareas, retrasa deliberadamente la respuesta de una medición en un entorno de prueba. Presenta y retira una tarjeta durante esa espera y comprueba que se registre al terminar el POST de medición. Repite con varias tarjetas y confirma el orden. Si se satura la cola, verifica los avisos en el monitor serie y vuelve a presentar la tarjeta omitida.
