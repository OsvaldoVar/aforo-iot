# S1-B2 · Prueba del DHT22

Este sketch del [Issue #10](https://github.com/OsvaldoVar/aforo-iot/issues/10) muestra la temperatura y la humedad cada 2 segundos en el monitor serie.

1. Con el ESP32 desconectado, conecta el DHT22 a 3.3 V y GND y lleva DATA a GPIO 4. Si el sensor carece de resistencia integrada, conecta una de 10 kΩ entre DATA y 3.3 V, según el [montaje del Issue #9](https://github.com/OsvaldoVar/aforo-iot/blob/586bfb8bccd6d35f91c0b984da8d3f5180bbed4e/docs/hardware/s1_b1_montaje_protoboard.md).
2. En Arduino IDE, instala el soporte para ESP32 y la biblioteca **DHT sensor library** de Adafruit con su dependencia **Adafruit Unified Sensor**.
3. Abre `prueba_dht22.ino`, selecciona la placa y el puerto, y carga el sketch. Abre el monitor serie a **115200 baudios**.
4. Comprueba que se muestra una línea con temperatura (°C) y humedad relativa (%HR) cada 2 segundos. Si una lectura es inválida, se muestra un aviso y el programa vuelve a intentar en el siguiente intervalo.

La comprobación con el sensor físico queda pendiente.
