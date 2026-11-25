# Conclusiones, limitaciones y trabajo futuro

## Conclusiones

El proyecto **Aforo IoT** integró un lector RFID, un sensor de temperatura y humedad, un backend PHP con MySQL y una aplicación Android para cubrir el control de acceso y el monitoreo del espacio. Al cierre del Sprint 3 los tres componentes funcionan en conjunto sobre la red local.

- La separación por capas (ESP32 → API PHP → base de datos → app Android) permitió avanzar por sprint sin rehacer trabajo: primero autenticación y consulta, después administración y finalmente controles, alertas y gráficas.
- El uso de un token de sesión para la app y de una API key por dispositivo mantiene separados los permisos de los usuarios y del hardware.
- Material Design 3 dio una interfaz consistente entre las pantallas del Sprint 1 y las del Sprint 3, incluyendo modo claro y oscuro.
- Las gráficas se alimentan de las mismas mediciones que usa el Dashboard, por lo que no requirieron tablas ni endpoints nuevos.
- Las notificaciones locales permitieron avisar de una alerta sin depender de servicios externos de mensajería.
- La exportación a CSV resolvió la entrega de datos al usuario desde el teléfono.

## Limitaciones

- La exportación CSV se genera en el teléfono y comparte el archivo con las apps instaladas; no queda un historial de descargas en el servidor.
- El monitoreo de alertas corre en un servicio en primer plano: si el sistema operativo lo detiene por ahorro de energía, los avisos se reanudan al abrir la app.
- Las gráficas muestran las últimas 24 horas y no permiten elegir otro periodo.
- Las pruebas de hardware se hicieron con el ESP32 y un DHT11 en el entorno local; la validación con el lector RFID físico quedó limitada a las tarjetas de prueba.
- La app administra un dispositivo activo a la vez para las pantallas de límites y gráficas.
- No hay pruebas automatizadas de interfaz; la verificación es manual y con compilación más lint en cada rama.

## Trabajo futuro

- Consumir `app/exportar.php` desde la app para exportar rangos completos y conservar un registro de exportaciones.
- Reemplazar el sondeo periódico por notificaciones push (FCM) y tareas programadas del sistema.
- Permitir elegir el rango de las gráficas y comparar periodos.
- Añadir selección de dispositivo en Límites y Gráficas para instalaciones con varios ESP32.
- Incorporar pruebas instrumentadas de los flujos principales y pruebas de carga del endpoint de mediciones.
- Publicar la app en un canal de distribución interno con firma de release.

## Referencias

- Android Developers. *Guide to app architecture* y *Notifications overview*. https://developer.android.com
- Material Design 3. *Components and theming*. https://m3.material.io
- Google. *Volley* (biblioteca HTTP para Android). https://google.github.io/volley
- PhilJay. *MPAndroidChart* v3.1.0. https://github.com/PhilJay/MPAndroidChart
- The PHP Group. *PHP Manual*. https://www.php.net/manual
- Oracle. *MySQL 8.0 Reference Manual*. https://dev.mysql.com/doc
- Espressif Systems. *ESP32 Series Datasheet*.
- NXP. *MFRC522 Standard Performance MIFARE and NTAG frontend*.
- Aosong. *DHT11 Humidity and Temperature Sensor Datasheet*.

## Revisión del equipo

| Integrante | Revisión |
| --- | --- |
| A (OsvaldoVar) | Pendiente |
| B (salazar5678-web) | Pendiente |
| C (OscarJDA) | Redactado, pendiente de revisión cruzada |
