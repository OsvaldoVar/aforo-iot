# Reporte de pruebas de la app Android - Sprint 3

Pruebas funcionales de la aplicación **Aforo IoT** realizadas sobre un dispositivo físico con el backend local, correspondientes a las tareas del Sprint 3 (controles del Dashboard, límites, alertas, gráficas, notificaciones y exportación).

## Entorno de pruebas

| Elemento | Detalle |
| --- | --- |
| Dispositivo | 23013PC75G, Android 17 |
| Aplicación | `com.example.aforoiot` versión 0.1 (debug), `minSdk 24`, `targetSdk 35` |
| Backend | PHP 8.3.37 con `php -S 0.0.0.0:8080 -t backend` |
| Base de datos | MySQL (XAMPP), base `aforo_iot` con datos de prueba |
| Red | Teléfono y equipo en la misma red Wi-Fi (`192.168.0.x`), URL `http://192.168.0.114:8080/api_aforo/` |
| Hardware | ESP32 simulado contra `esp32/medicion.php` con la API key del dispositivo 1 |

Cada entrega se verificó con `gradlew :app:assembleDebug :app:lintDebug`; todas las ramas del Sprint 3 compilan y pasan lint sin errores.

## Pruebas de pantallas

| # | Caso | Pasos | Resultado esperado | Estado |
| --- | --- | --- | --- | --- |
| 1 | Inicio de sesión | Credenciales válidas | Entra al Dashboard y conserva la sesión | OK |
| 2 | Sesión persistente | Cerrar y abrir la app | Entra directo al Dashboard | OK |
| 3 | Dashboard con datos | Abrir Dashboard con mediciones registradas | Muestra dentro, aforo, temperatura, humedad y últimas mediciones | OK |
| 4 | Abrir acceso | Tocar **Abrir acceso** y confirmar | Encola el comando y muestra aviso de resultado | OK |
| 5 | Activar alarma | Tocar **Activar alarma** y confirmar | Encola el comando y muestra aviso de resultado | OK |
| 6 | Control sin conexión | Ejecutar un control con el servidor detenido | Aviso de error, sin bloquear la pantalla | OK |
| 7 | Historial de accesos | Abrir Historial, pestaña Accesos | Lista con nombre, tipo y fecha | OK |
| 8 | Historial de mediciones | Cambiar a pestaña Mediciones | Lista con temperatura, humedad y fecha | OK |
| 9 | Rango de fechas | Tocar **Todo el historial** y elegir un rango | Recarga la lista con el rango elegido | OK |
| 10 | Historial vacío | Consultar un rango sin registros | Muestra el estado vacío | OK |
| 11 | Exportar CSV | Tocar **Exportar CSV** en Accesos o Mediciones | Genera `historial.csv` y abre la hoja de compartir | OK |
| 12 | Exportar sin datos | Exportar en una pestaña vacía | Aviso "No hay datos para exportar" | Revisado en código |
| 13 | Personas | Abrir Personas | Lista personas activas y tarjetas pendientes | OK |
| 14 | Dispositivos | Abrir Más → Dispositivos | Lista dispositivos y permite copiar la API key | OK |
| 15 | Límites: carga | Abrir Más → Límites | Campos con los límites actuales del dispositivo | OK |
| 16 | Límites: validación | Guardar temperatura fuera de 0-60, humedad fuera de 0-100 o aforo menor a 1 | Marca el campo con error y no envía | OK |
| 17 | Límites: guardar | Guardar valores válidos | Mensaje "Límites guardados" y se reflejan en el backend | OK |
| 18 | Alertas: listado | Abrir Más → Alertas | Lista alertas con tipo, fecha y estado | OK |
| 19 | Alertas: atender | Marcar una alerta como atendida | Se actualiza el estado en pantalla y en la base | OK |
| 20 | Gráficas | Abrir Más → Gráficas | Línea de temperatura/humedad y barras de entradas por hora de las últimas 24 h | OK |
| 21 | Navegación Más | Abrir cada tarjeta de Más y volver | Cada tarjeta abre su pantalla y el retroceso regresa a Más | OK |
| 22 | Cerrar sesión | Tocar el icono de salida y confirmar | Vuelve al login y detiene el monitoreo | OK (detiene el servicio) |

Evidencia de las pantallas nuevas: [Dashboard con controles](../capturas/s3_dashboard_controles.png), [Límites](../capturas/s3_limites.png), [Alertas](../capturas/s3_alertas.png), [Gráficas](../capturas/s3_graficas.png), [Historial](../capturas/s3_historial.png), [Exportar CSV](../capturas/s3_exportar.png), [Más](../capturas/s3_mas.png), [Personas](../capturas/s3_personas.png).

## Pruebas de errores de red

| # | Caso | Pasos | Resultado observado | Estado |
| --- | --- | --- | --- | --- |
| 23 | Servidor detenido al guardar | Detener PHP y guardar límites | Muestra "No se pudo conectar con el servidor"; el botón se habilita de nuevo | OK con observación |
| 24 | Servidor detenido al abrir | Detener PHP y abrir Límites | La pantalla abre vacía y el aviso aparece después del reintento de la petición | OK con observación |
| 25 | Token inválido (401) | Invalidar el token en la base y guardar límites | Cierra la sesión y vuelve al login sin mostrar datos | OK |
| 26 | Vuelta a la normalidad | Restaurar el servidor y volver a iniciar sesión | Login correcto, Dashboard y pantallas con datos | OK |

Observación: el aviso de error tarda entre 15 y 20 segundos porque la petición usa un tiempo límite de 10 segundos y un reintento. No se pierde información y el usuario puede reintentar.

Evidencia: [aviso de error de conexión](../capturas/s3_error_red.png), [sesión expirada](../capturas/s3_sesion_expirada.png).

## Pruebas de notificaciones

| # | Caso | Pasos | Resultado esperado | Estado |
| --- | --- | --- | --- | --- |
| 27 | Permiso de notificaciones | Instalar y entrar por primera vez en Android 13+ | Solicita el permiso y continúa si se acepta | OK |
| 28 | Canal de alertas | Revisar los canales de la app | Canal "Alertas de Aforo IoT" con importancia alta | OK |
| 29 | Alerta nueva | Registrar una medición que supere el límite de temperatura | Notificación "Nueva alerta" con el mensaje del servidor | OK |
| 30 | Alerta repetida | Consultar varias veces la misma alerta | No se vuelve a notificar | Revisado en código |
| 31 | Servicio en primer plano | Salir de la app con la sesión activa | La notificación "Vigilando alertas del sistema" sigue activa | OK |
| 32 | Cierre de sesión | Cerrar sesión | Se detiene el servicio y desaparece la notificación de monitoreo | OK |

Evidencia: [notificación de alerta](../capturas/s3_notificacion.png).

## Defectos encontrados y corregidos

| Defecto | Causa | Corrección |
| --- | --- | --- |
| Al entrar se notificaban alertas viejas ya vistas | La primera consulta no guardaba la línea base de alertas | Se registra el último ID al arrancar el monitoreo |
| El monitoreo se detenía al cerrar la app | El monitoreo vivía en la actividad principal | Se movió a un servicio en primer plano con notificación persistente |

## Limitaciones de la ronda de pruebas

- Las mediciones del ESP32 se enviaron por HTTP al endpoint `esp32/medicion.php`, no desde el hardware final; la prueba con el sensor DHT11 se hizo por separado en el entorno local del equipo.
- No se probó con varios usuarios simultáneos ni con 24 horas continuas de datos; las gráficas se validaron con los registros disponibles.
- La app exporta en el teléfono los registros visibles (máximo 100). El servidor también ofrece `app/exportar.php` (hasta 10 000 filas), probado en la colección Postman.
- Si Android detiene el servicio en segundo plano por ahorro de energía, el monitoreo se reanuda al volver a abrir la app.
