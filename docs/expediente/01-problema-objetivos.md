# 1. Problema, justificación, objetivos y alcance

**Proyecto:** Aforo IoT — Sistema de control de acceso con RFID y monitoreo ambiental  
**Fecha:** Noviembre de 2026  
**Responsable:** OsvaldoVar

---

## 1.1 Planteamiento del problema

En espacios cerrados de uso frecuente —como laboratorios universitarios, aulas académicas y locales comerciales o de atención al público— el control de aforo y la vigilancia de las condiciones ambientales suelen realizarse de forma manual o simplemente se omiten. El conteo manual mediante bitácoras en papel o registros improvisados presenta varias deficiencias operativas:

- **Falta de visibilidad en tiempo real:** los responsables del espacio desconocen cuántas personas se encuentran dentro en un momento dado y quién ingresó o salió.
- **Incumplimiento de límites de capacidad:** sin un mecanismo automatizado que verifique el aforo máximo, se producen aglomeraciones que comprometen la seguridad, la comodidad y las normas internas del recinto.
- **Ausencia de monitoreo ambiental continuo:** variaciones excesivas de temperatura o humedad relativa (que afectan tanto el confort de los ocupantes como la integridad de equipos electrónicos en laboratorios) pasan inadvertidas al no existir registro ni alertas oportunas.
- **Trazabilidad deficiente:** los registros en papel son propensos a errores humanos, pérdida de información y dificultan la consulta histórica o la generación de reportes para auditoría y toma de decisiones.

Ante esta situación, resulta necesario contar con una solución tecnológica integrada que automatice el registro de entradas y salidas, supervise en tiempo real la temperatura y la humedad del recinto, y centralice la información en una plataforma accesible desde dispositivos móviles.

---

## 1.2 Justificación

El desarrollo de **Aforo IoT** responde a la necesidad práctica de modernizar la gestión de espacios cerrados mediante tecnologías accesibles de Internet de las Cosas (IoT) y desarrollo móvil:

- **Impacto operativo y seguridad:** al automatizar el conteo de ocupantes con tarjetas RFID y aplicar reglas de aforo máximo desde un servidor central, se evita la saturación del espacio y se ofrece retroalimentación inmediata en el punto de acceso mediante indicadores visuales y sonoros.
- **Control ambiental preventivo:** la lectura periódica de temperatura y humedad con un sensor DHT22 permite detectar condiciones anómalas de forma temprana y notificar al administrador cuando se superan los umbrales configurados.
- **Accesibilidad y centralización:** el uso de una API REST respaldada por una base de datos relacional desacopla el hardware del cliente móvil, garantiza la persistencia del historial y permite consultar el estado del recinto, gráficas de comportamiento y alertas desde una aplicación Android con interfaz Material Design 3.
- **Pertinencia académica y viabilidad económica:** el proyecto integra hardware embebido (ESP32), desarrollo backend (PHP 8 y MySQL) y desarrollo móvil nativo (Android Java) utilizando componentes de bajo costo y software de libre distribución, lo que lo hace replicable en entornos educativos y pequeños establecimientos.

---

## 1.3 Objetivo general

Diseñar, implementar y validar un sistema IoT integral denominado **Aforo IoT** que permita controlar el acceso mediante tecnología RFID, monitorear en tiempo real la temperatura y humedad de un espacio cerrado y gestionar alertas y acciones remotas desde una aplicación móvil Android comunicada a través de una API REST con base de datos relacional.

---

## 1.4 Objetivos específicos

1. **Construir el nodo IoT de adquisición y respuesta local** sobre un microcontrolador ESP32 con lector RFID RC522, sensor DHT22, LEDs indicador (verde/rojo) y buzzer, capaz de leer tarjetas con antirrebote de 3 s, enviar mediciones ambientales cada 15 s por HTTP/JSON y emitir retroalimentación local inmediata.
2. **Desarrollar una API REST en PHP 8 con persistencia en MySQL** que autentique dispositivos mediante `api_key` y usuarios mediante token (`X-Token`), aplique la lógica de alternancia entrada/salida con validación de `aforo_max`, registre alertas automáticas y responda consultas históricas en menos de 1 segundo para volúmenes de al menos 1 000 registros.
3. **Implementar una aplicación móvil Android en Java con Material Design 3** que permita iniciar sesión, visualizar el aforo actual, temperatura, humedad y estado de conexión del ESP32 con actualización automática cada 5 s, consultar historiales filtrados por fecha, graficar tendencias de 24 horas y exportar reportes en formato CSV.
4. **Integrar un mecanismo de alertas y control remoto bidireccional** que notifique al usuario cuando se excedan los límites configurados de temperatura, humedad o aforo máximo, y que permita encolar comandos (`abrir_acceso` y `alarma`) ejecutados por el ESP32 con una latencia inferior a 4 segundos.
5. **Validar el funcionamiento integral del sistema** mediante pruebas documentadas de API (Postman), pruebas físicas de hardware (al menos 20 lecturas consecutivas, reconexión ante corte de WiFi y tolerancia a fallos del sensor) y pruebas funcionales de la aplicación móvil.

---

## 1.5 Alcance

### Incluye

- **Hardware y firmware:** montaje en protoboard con ESP32, lector RC522 (3.3 V), sensor DHT22, LED verde, LED rojo y buzzer; conexión WiFi con reconexión automática; envío de eventos RFID y mediciones por HTTP/JSON; consulta periódica de comandos remotos.
- **Backend y base de datos:** base de datos MySQL (`aforo_iot`) con tablas de usuarios, personas, tarjetas pendientes, dispositivos, registros de acceso, mediciones, límites, alertas, comandos y vista `personas_dentro`; API REST en PHP 8 con PDO y autenticación diferenciada (`api_key` para ESP32 y `X-Token` para la app).
- **Lógica de acceso y alertas:** alternancia automática entrada/salida controlada en el servidor; registro de tarjetas desconocidas como pendientes para asignación desde la app; bloqueo de entradas al alcanzar `aforo_max` permitiendo siempre salidas; generación automática de alertas por aforo, temperatura o humedad.
- **Aplicación Android:** registro e inicio de sesión, tablero en vivo (polling cada 5 s), listado de personas dentro, historial con selector de rango de fechas, administración de personas y dispositivos, configuración de límites, gestión de alertas con notificaciones locales, gráficas de 24 h, acciones de control remoto y exportación a CSV.
- **Documentación y pruebas:** expediente técnico, diagramas de arquitectura, despliegue, ER, conexiones y secuencia, colección Postman y reportes de pruebas.

### No incluye

- **Actuación sobre cerradura física real en producción:** la acción de apertura de acceso se demuestra mediante LED o relevador de prueba en protoboard, sin instalación de chapa electromagnética ni obra eléctrica en puertas reales.
- **Operación multi-sede o despliegue en la nube:** el sistema está diseñado y probado para un único sitio físico operando dentro de una red de área local (LAN); no incluye alojamiento en servidor público, dominio DNS ni cifrado TLS/HTTPS en esta versión.
- **Integración con sistemas institucionales externos:** no se conecta con plataformas de control escolar, nómina, torniquetes comerciales ni sistemas de videovigilancia.
- **Soporte multiplataforma móvil:** la aplicación cliente se desarrolla exclusivamente para Android (Java); no incluye cliente iOS ni aplicación web de administración.
- **Respaldo energético del nodo:** no se contempla alimentación con batería ni UPS para el ESP32 o el servidor local.

---

## 1.6 Supuestos y restricciones

### Supuestos

- El servidor local (laptop con XAMPP), el microcontrolador ESP32 y el teléfono Android operan conectados a la misma red WiFi de 2.4 GHz.
- Las tarjetas RFID utilizadas son compatibles con el estándar ISO/IEC 14443A soportado por el módulo RC522 y poseen identificadores (UID) únicos y estables.
- Cada persona porta y presenta su propia tarjeta tanto al ingresar como al salir del espacio monitoreado.
- El firewall del equipo servidor permite tráfico entrante en el puerto TCP 80 para los dispositivos de la red local.

### Restricciones

- **Calendario de entrega:** el proyecto completo (código, prototipo físico, pruebas y expediente técnico) debe concluirse en un plazo estricto de 7 días, del 20 al **26 de noviembre de 2026**.
- **Presupuesto y disponibilidad de hardware:** se restringe al uso de componentes electrónicos de bajo costo para prototipado (ESP32, RC522 con 3 tarjetas, DHT22, LEDs con resistencias de 220 Ω, buzzer y protoboard), contemplando simulación en Wokwi como plan de contingencia ante fallas físicas.
- **Equipo de trabajo:** el desarrollo se distribuye entre 3 integrantes con responsabilidades compartidas en al menos dos capas del sistema por persona.
- **Restricciones técnicas de plataforma:** el ESP32 solo soporta WiFi en banda de 2.4 GHz; el módulo RC522 debe alimentarse estrictamente a 3.3 V; Android 9 o superior bloquea tráfico HTTP sin cifrar por defecto, por lo que la app requiere configuración explícita de `network_security_config` para la red local.
