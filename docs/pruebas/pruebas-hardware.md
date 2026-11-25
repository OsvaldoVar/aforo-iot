# Pruebas de hardware

Pruebas del [Issue #81](https://github.com/OsvaldoVar/aforo-iot/issues/81), realizadas el 25 de noviembre de 2026.

**Equipo:** ESP32 DevKit, lector RC522, sensor DHT11, LED verde (GPIO 25), LED rojo (GPIO 26) y buzzer (GPIO 27), con la API local y la app Android en la misma red WiFi.

**Firmware:** `aforo_config` de prueba, que reúne la lógica de los PR #73, #74 y #75 más la consulta de comandos de #78 y las alertas de #79. El firmware de `develop` todavía no integra RFID, LEDs ni comandos.

## Tabla de resultados

| # | Prueba | Pasos | Resultado esperado | Resultado obtenido | ¿Pasa? |
| --- | --- | --- | --- | --- | --- |
| 1 | 20 lecturas seguidas | Pasa 2 tarjetas 10 veces cada una, esperando 4 s entre lecturas | 20 registros alternando entrada y salida en Historial | 20 registros: Ana (ids 19–28) y Carlos (ids 29–38), todos alternando entrada/salida entre 10:21:39 y 10:23:39 | Pasa |
| 2 | Antirrebote | Deja la tarjeta encima del lector 10 s | 1 solo registro | Un solo registro (id 39, Sofía entrada 10:23:48) con la tarjeta sostenida 10 s | Pasa |
| 3 | Tarjeta desconocida | Pasa una tarjeta sin registrar | LED rojo + pitidos; aparece en Personas → pendientes | UID `12121212`, `19281038` y `9749A07F` en `tarjetas_pendientes`; LED rojo y pitidos | Pasa |
| 4 | Aforo lleno | `aforo_max = 2`, 3 personas entran | La tercera ve LED rojo y no se registra | Ana entró (id 40, 10:51:00) y quedaron 2 dentro; Carlos fue rechazado sin registro, con alerta de aforo (id 2, 10:51:10) y notificación en la app | Pasa |
| 5 | Corte de WiFi | Apaga el router o el hotspot 30 s y vuelve a encenderlo | El ESP32 se reconecta solo en menos de 20 s | No ejecutada: reiniciar el router cambia la IP de la laptop y rompería la URL configurada en la app y el ESP32 | No ejecutada |
| 6 | API caída | Detén `php -S` 30 s y vuelve a iniciarlo | El ESP32 reintenta y vuelve a enviar mediciones | API detenida 10:52:17–10:52:47; las mediciones se reanudaron solas a las 10:53:05 y siguieron cada 15 s | Pasa |
| 7 | DHT desconectado | Desconecta el cable DATA del sensor | El monitor serie avisa del error; las tarjetas siguen funcionando | El monitor mostró `Lectura invalida del sensor DHT` y el acceso de Ana se registró (id 41, 10:55:56); al reconectar volvió la medición | Pasa |
| 8 | Abrir acceso | Botón en la app | LED verde en menos de 4 s | Comando id 4 creado y ejecutado a las 10:20:52 (0 s); el firmware enciende el LED verde 3 s | Pasa |
| 9 | Activar alarma | Botón en la app | Buzzer en menos de 4 s | Comando id 5 creado 10:20:57 y ejecutado 10:20:59 (2 s); el buzzer sonó | Pasa |

## Registros

### 1. 20 lecturas seguidas

Veinte lecturas consecutivas, diez por tarjeta, con 4 s entre cada una. La alternancia entrada/salida se mantuvo en todas:

```
ids 19–28 | Ana    A1B2C3D4 | 10:21:39 a 10:22:37 | entrada/salida alternadas
ids 29–38 | Carlos B2C3D4E5 | 10:22:56 a 10:23:39 | entrada/salida alternadas
```

### 2. Antirrebote

Una sola lectura tras mantener la tarjeta sobre el lector durante 10 s:

```
id 39 | Sofia Ramirez | C3D4E5F6 | entrada | 10:23:48
```

### 3. Tarjeta desconocida

Las tres UID sin registrar quedaron en `tarjetas_pendientes` y el lector respondió con LED rojo y pitidos:

```
12121212  09:11:04 y 09:11:08
19281038  09:11:12 y 09:18:23
9749A07F  09:11:17, 09:11:23 y 09:14:07
```

### 4. Aforo lleno

Con `aforo_max = 2` y Sofía dentro, Ana entró y Carlos fue rechazado:

```
id 40 | Ana Martinez | entrada | 10:51:00   (dentro = 2)
Carlos: sin registro en registros_acceso
alerta id 2 | tipo aforo | "Aforo lleno, acceso denegado" | 10:51:10
```

La app mostró la notificación de aforo lleno.

### 5. Corte de WiFi

No ejecutada. Reiniciar el router cambia la IP asignada a la laptop y con ella la URL configurada en el ESP32 y en la app, lo que rompería el entorno de la demo. Queda pendiente repetirla después de la entrega con una IP fija o una reserva DHCP, o desactivando únicamente la radio de 2.4 GHz desde el panel del router.

### 6. API caída

```
10:52:17  API detenida
10:52:47  API reiniciada
10:53:05  medición id 241 (23.2 °C, 43 %)
10:53:20  medición id 242
10:53:35  medición id 243
```

El ESP32 no se quedó bloqueado: al volver la API reanudó las mediciones cada 15 s.

### 7. DHT desconectado

Con el cable DATA del DHT desconectado, el monitor serie mostró lecturas inválidas mientras las tarjetas siguieron funcionando:

```
Medicion registrada: 23.4 C, 43.0 % (HTTP 200)
Lectura invalida del sensor DHT. Medicion omitida.
Lectura invalida del sensor DHT. Medicion omitida.
Acceso: salida registrada.
Lectura invalida del sensor DHT. Medicion omitida.
Medicion registrada: 23.4 C, 43.0 % (HTTP 200)
```

En la base, el acceso de Ana quedó registrado (id 41, salida 10:55:56) y el hueco de mediciones va de 10:55:16 a 10:56:16; al reconectar el sensor volvieron a llegar cada 15 s.

### 8. Abrir acceso

Desde el botón de la app:

```
id 4 | abrir_acceso | creado 10:20:52 | ejecutado 10:20:52 | 0 s
```

El firmware enciende el LED verde durante 3 s.

### 9. Activar alarma

Desde el botón de la app:

```
id 5 | activar_alarma | creado 10:20:57 | ejecutado 10:20:59 | 2 s
```

El buzzer sonó durante la señal de alarma.

## Fotos

- [Montaje](../capturas/hw_montaje.png)
- [Material](../capturas/hw_prueba.png)

## Notas

- El sensor del kit es un DHT11, no un DHT22. Con `DHT22` la librería devuelve `NaN` y no se registran mediciones; el firmware de prueba se compiló con `DHT11`.
- La lectura de tarjetas reconocidas se hizo con emulación NFC de las UID del seed; las tarjetas físicas del kit quedan pendientes de comparar contra esos valores.
