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

## Señalización de acceso

| Resultado | Señal |
| --- | --- |
| Entrada o salida confirmada | LED verde durante 2 s, sin sonido. |
| Acceso denegado, tarjeta desconocida o respuesta sin confirmación | LED rojo durante 2 s y dos pitidos de 100 ms separados por 100 ms. |
| Reposo | Ambos LEDs apagados y buzzer en silencio. |

Conecta el LED verde a GPIO 25 y el rojo a GPIO 26, cada uno con una resistencia de 220 Ω. El buzzer piezoeléctrico del diagrama usa GPIO 27 y un tono de 2 kHz. Verifica la tensión y corriente del buzzer físico antes de conectarlo y usa un circuito de control si el modelo lo requiere. [Montaje en protoboard](../../docs/hardware/s1_b1_montaje_protoboard.md)

Queda pendiente conectar `indicarRespuestaAcceso(codigoHttp, cuerpo)` a la llamada real a `esp32/acceso.php` del [Issue #43](https://github.com/OsvaldoVar/aforo-iot/issues/43). El LED verde requiere HTTP 200, `success: true` booleano y `tipo: entrada` o `salida`. Los fallos de medición del DHT22 siguen su flujo independiente.

Tras integrar el lector, prueba una tarjeta autorizada, una desconocida y una lectura con la API desconectada. Comprueba el color, los pitidos, el retorno a reposo y el cambio entre resultados seguidos. Verifica en el monitor serie que continúen las mediciones y graba un video corto de los casos permitido y denegado para el [Issue #45](https://github.com/OsvaldoVar/aforo-iot/issues/45).
