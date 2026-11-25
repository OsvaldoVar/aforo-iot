# Guion de demostración en vivo — Aforo IoT

Guion operativo para la presentación y demostración en vivo del sistema **Aforo IoT** con una duración total de **8 minutos**.

**Equipo:**

- **Osvaldo** (`OsvaldoVar`) — Base de datos, API REST y administración del repositorio.
- **Diego** (`salazar5678-web`) — Firmware ESP32 y montaje de hardware.
- **Oscar** (`OscarJDA`) — Aplicación Android y pruebas de hardware.

---

## Guion de la demo (8 minutos)

| Minuto | Quién | Qué hace | Qué debe verse |
| --- | --- | --- | --- |
| **0:00 – 1:00** | **Osvaldo** | Presenta el problema del control manual de aforo y clima en espacios cerrados, los objetivos del proyecto y la arquitectura de cuatro capas (`ESP32 → API PHP 8 → MariaDB → App Android`) con autenticación separada (`api_key` y `X-Token`). | Diapositivas de portada, problema/objetivos y diagrama de arquitectura; consola con `php -S 0.0.0.0:8080 -t backend` activo y el Dashboard en línea en el teléfono. |
| **1:00 – 1:50** | **Diego** | Muestra brevemente el montaje en protoboard (ESP32, RC522 a 3.3 V, DHT11, LED verde, LED rojo y buzzer) y acerca una tarjeta RFID registrada al lector RC522. | En el ESP32 se enciende el **LED verde** (2 s) sin buzzer y el monitor serie imprime `Acceso: entrada registrada.`; en el Dashboard de la app el contador de personas dentro sube en menos de 5 s y la persona aparece en la lista de ocupantes. |
| **1:50 – 2:25** | **Diego** | Espera al menos 3 s (ventana de antirrebote) y vuelve a acercar la **misma tarjeta** al lector RC522. | En el ESP32 se enciende el **LED verde** (2 s) y el monitor serie imprime `Acceso: salida registrada.`; en el Dashboard el contador disminuye y la persona sale de la lista de ocupantes dentro. |
| **2:25 – 3:10** | **Diego** y **Oscar** | Diego acerca una **tarjeta desconocida** (no registrada en `personas`) al lector RC522. Oscar abre la pestaña **Personas** en la app. | En el ESP32 se enciende el **LED rojo** (2 s) con **2 pulsos de buzzer** (100 ms); en la app, dentro de **Personas → Tarjetas pendientes**, aparece el UID hexadecimal en mayúsculas recién leído. |
| **3:10 – 3:55** | **Oscar** | Selecciona la tarjeta pendiente en **Personas**, captura el nombre de la nueva persona y guarda la asignación. | La tarjeta desaparece de la sección de pendientes y pasa al listado de personas registradas activas con su UID vinculado; si Diego vuelve a pasarla, ahora se registra como acceso permitido (LED verde). |
| **3:55 – 4:55** | **Oscar** y **Diego** | Oscar entra a **Más → Límites**, configura **`aforo_max = 1`** y guarda (asegurando 1 persona dentro). Diego acerca **otra tarjeta registrada** para intentar ingresar. | En el ESP32 se enciende el **LED rojo** con pitidos y el monitor serie muestra `Acceso: denegado por aforo lleno.`; la entrada no se registra en `registros_acceso`, aparece la alerta de tipo `aforo` en **Más → Alertas** y llega la **notificación local** (`Nueva alerta`) en Android. |
| **4:55 – 5:45** | **Oscar** y **Diego** | Desde el **Dashboard**, Oscar pulsa **Abrir acceso** y confirma el diálogo; enseguida pulsa **Activar alarma** y confirma. Diego enfoca el hardware. | Ambos comandos se ejecutan en **menos de 4 s** (sondeo del ESP32 cada 3 s): con **Abrir acceso** se enciende el **LED verde** por 3 s; con **Activar alarma** se enciende el **LED rojo** y suena el **buzzer** continuo por 3 s. La app muestra el Snackbar de confirmación. |
| **5:45 – 6:35** | **Oscar** | Abre **Más → Gráficas** (últimas 24 h: líneas de temperatura/humedad y barras de entradas por hora). Luego entra a **Historial** (pestañas Accesos y Mediciones), muestra el selector de rango y pulsa **Exportar CSV** para compartir/abrir el archivo `historial.csv`. | Se visualizan ambas gráficas con datos reales; en **Historial** se ven los movimientos recién generados y al exportar se abre la hoja de compartir de Android con el archivo CSV (UTF-8 con acentos legibles correctamente). |
| **6:35 – 7:25** | **Osvaldo** | Ejecuta la colección de pruebas de la API en Postman / Newman (`newman run docs/postman/aforo-iot.postman_collection.json --env-var 'base_url=http://localhost:8080/api_aforo'`) y muestra el resumen final en consola. | Resumen de ejecución en verde: **84 peticiones** ejecutadas, **120 aserciones** evaluadas y **0 fallas** (100 % de éxito cubriendo RF01–RF08, códigos 400/401, alertas, comandos y exportación CSV). |
| **7:25 – 8:00** | **Osvaldo, Diego y Oscar** | Presentan brevemente las conclusiones, limitaciones identificadas y líneas de trabajo futuro (diapositiva 8) y abren espacio para preguntas. | Diapositiva de conclusiones y trabajo futuro en pantalla; equipo listo para responder preguntas técnicas sobre BD/API, hardware/firmware y app Android. |

---

## Preparación (30 min antes)

Realizar esta verificación completa al menos 30 minutos antes de iniciar la demostración:

- [ ] **Servidor backend en ejecución:** desde la raíz del repositorio, levantar PHP embebido en todas las interfaces:
  ```bash
  php -S 0.0.0.0:8080 -t backend
  ```
  Confirmar que MariaDB está activo y que `http://localhost:8080/api_aforo/index.php` responde `success: true`.
- [ ] **Reset de la base de datos (opcional):** si se desea iniciar con datos limpios y controlados para la demo y Newman, recargar el esquema y los datos semilla:
  ```bash
  mariadb -u root -p aforo_iot < database/schema.sql
  mariadb -u root -p aforo_iot < database/seed.sql
  ```
- [ ] **IP de la laptop confirmada:** obtener la IP local en la red WiFi de 2.4 GHz (`ip -4 addr` o `hostname -I`) y verificar que el firewall permita tráfico entrante en el puerto TCP `8080`.
- [ ] **Firmware actualizado desde `develop`:** flashear `firmware/esp32_aforo/esp32_aforo.ino` en el ESP32 (placa *ESP32 Dev Module*, sensor `DHT11` por defecto).
- [ ] **URL del servidor configurada en la app y en el ESP32:**
  - **App Android:** en **Login → Configurar servidor** (o **Más → Servidor** si ya hay sesión iniciada), ingresar `http://<IP-de-la-laptop>:8080/api_aforo/`.
  - **ESP32:** abrir el monitor serie a `115200` baudios, escribir `m` + Enter y usar las opciones del menú serial (`1` red WiFi 2.4 GHz, `2` IP y puerto del servidor, `6` guardar en NVS y conectar) sin necesidad de recompilar.
- [ ] **Verificar enlace completo:** comprobar en el monitor serie `WiFi conectado.` y `Medicion registrada`, y en el Dashboard de la app que el chip muestre **ESP32 en línea**.
- [ ] **Límite `aforo_max` en valor normal:** confirmar en **Más → Límites** que `aforo_max` esté en su valor normal (por ejemplo `20`) antes de comenzar, para poder bajarlo a `1` en vivo durante el minuto 3:55.
- [ ] **Notificaciones del teléfono activadas:** verificar que el permiso de notificaciones esté concedido (Android 13+), el canal **Alertas de Aforo IoT** tenga importancia alta, el modo *No molestar* esté desactivado y aparezca la notificación persistente del servicio en primer plano (`Vigilando alertas del sistema`).
- [ ] **Tarjetas listas y separadas:** tener identificadas a la mano dos tarjetas registradas (para entrada/salida y prueba de aforo lleno) y una tarjeta sin registrar (para tarjeta desconocida y asignación en vivo).
- [ ] **Presentación abierta:** tener lista `docs/presentacion/aforo-iot.pdf` (o `docs/presentacion/aforo-iot.html`) y la terminal preparada para el comando de `newman`.

---

## Plan B (contingencias)

| Escenario de fallo | Acción inmediata |
| --- | --- |
| **Falla el WiFi del salón o el router bloquea tráfico entre dispositivos (AP isolation)** | Activar el **hotspot del teléfono en banda de 2.4 GHz** y conectar ahí la laptop y el ESP32. Obtener la nueva IP de la laptop y cambiar la URL del servidor **sin recompilar**: en la app desde **Login → Configurar servidor** (o **Más → Servidor**), y en el ESP32 abriendo el menú serial con la tecla **`m` + Enter** (opción `1` para elegir el hotspot, opción `2` para ingresar la nueva IP y puerto `8080`, opción `6` para guardar en NVS y conectar). |
| **Falla física del ESP32, lector RC522 o cableado** | Mostrar la evidencia documentada de las pruebas físicas en [`docs/pruebas/pruebas-hardware.md`](pruebas/pruebas-hardware.md) (8 de 9 pruebas aprobadas y prueba 5 justificada, con marcas de tiempo y registros reales) junto con las fotos en `docs/hardware/fotos/`. Ejecutar en vivo las peticiones equivalentes del ESP32 desde **Postman** (`POST esp32/acceso.php`, `POST esp32/medicion.php` y `GET esp32/comando.php`) para demostrar la reacción inmediata de la API, la base de datos y la app Android. |
| **Falla el teléfono físico Android** | Abrir la app en el **emulador de Android Studio** en la misma laptop y configurar la URL del servidor en **Login → Configurar servidor** como `http://10.0.2.2:8080/api_aforo/` (alias del host local desde el emulador). Todas las pantallas, controles, historial, exportación CSV y notificaciones locales funcionan sobre ese entorno. |
