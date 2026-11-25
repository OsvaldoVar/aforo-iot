# Reporte de pruebas de la API

Pruebas automatizadas de integración y funcionales de la API REST del sistema **Aforo IoT** ejecutadas mediante Newman sobre la colección oficial de Postman, correspondientes a los módulos de autenticación, control de acceso RFID, telemetría ambiental, gestión de dispositivos y personas, configuración de límites, administración de alertas, comandos remotos y exportación de datos.

## 1. Objetivo y alcance

### Objetivo
Validar el correcto funcionamiento de los endpoints de la API REST del backend de **Aforo IoT**, verificando códigos de estado HTTP, contratos de datos JSON, mecanismos de autenticación y autorización segregada (`X-Token` y `api_key`), integridad referencial, reglas de negocio y respuestas ante condiciones límite y entradas inválidas.

### Alcance
El conjunto de pruebas automatizadas evalúa la totalidad de los servicios expuestos por el backend:
- **Autenticación y usuarios:** Registro, inicio de sesión y validación de tokens.
- **Telemetría e interacción IoT (ESP32):** Recepción de mediciones DHT22, control de acceso RFID con alternancia entrada/salida y consulta de comandos remotos.
- **Monitoreo de la aplicación móvil:** Tablero de estado, aforo en tiempo real y catálogo de personas dentro del recinto.
- **Gestión administrativa:** Altas, modificaciones, activaciones, desactivaciones de dispositivos ESP32 y personas, así como vinculación de tarjetas pendientes.
- **Límites y alertas (Sprint 3):** Umbrales de temperatura, humedad y aforo por dispositivo; generación automática de alertas sin duplicidad, atención de alertas y rechazo de accesos por aforo máximo sin registros redundantes.
- **Comandos remotos (Sprint 3):** Encolado y despacho con entrega única hacia el microcontrolador.
- **Exportación de datos:** Generación y descarga de archivos CSV con codificación UTF-8 BOM y mitigación de inyección de fórmulas.

### Entorno de pruebas

| Elemento | Detalle |
| --- | --- |
| Servidor backend | PHP 8.5 (servidor embebido con `php -S 0.0.0.0:8080 -t backend`) |
| Gestor de base de datos | MariaDB local, base de datos `aforo_iot` |
| Ejecutor de pruebas | Newman 6.2.1 (CLI de Postman) |
| Colección de pruebas | `docs/postman/aforo-iot.postman_collection.json` |
| Fecha de ejecución | 2026-11-24 |
| Entorno de red | Red local (`localhost` / `127.0.0.1`) |
| Estado de BD previo | Reiniciada con `database/schema.sql` y `database/seed.sql` |

### Procedimiento de reproducción
Para reproducir idénticamente los resultados de la suite de pruebas desde la raíz del repositorio:

1. **Reiniciar la base de datos** aplicando el esquema DDL y los datos iniciales semilla:
   ```bash
   mysql -u root -p aforo_iot < database/schema.sql
   mysql -u root -p aforo_iot < database/seed.sql
   ```

2. **Iniciar el servidor embebido de PHP** en el puerto 8080 apuntando al directorio `backend`:
   ```bash
   php -S 0.0.0.0:8080 -t backend
   ```

3. **Ejecutar la suite completa con Newman** proporcionando la variable de entorno `base_url`:
   ```bash
   newman run docs/postman/aforo-iot.postman_collection.json \
     --env-var 'base_url=http://localhost:8080/api_aforo'
   ```

---

## 2. Resumen de ejecución

La corrida automatizada del 2026-11-24 se ejecutó sobre la colección oficial `Aforo IoT - Sprint 1` contra el servidor embebido PHP 8.5 y MariaDB local previamente reiniciada con `database/schema.sql` y `database/seed.sql`:

| Métrica | Valor |
| --- | --- |
| Peticiones ejecutadas | 82 |
| Peticiones fallidas | 0 |
| Total de aserciones evaluadas | 118 |
| Aserciones fallidas | 0 |
| Tasa de éxito | 100 % |
| Duración total de la corrida | 1652 ms (1.65 s) |
| Tiempo de respuesta promedio | 7.7 ms |

---

## 3. Resultados agrupados por carpeta de la colección

### Carpeta: API

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-01 | Estado de la API | `GET api_aforo/index.php` | HTTP 200; Código de estado 200; Respuesta con success true y campos de diagnóstico | HTTP 200 (11 ms); Código de estado 200; Respuesta con success true y campos de diagnóstico | Pasa |

### Carpeta: Auth

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-02 | Registro OK | `POST api_aforo/auth/registro.php` | HTTP 201; Código de estado 201; Usuario registrado con campos clave | HTTP 201 (145 ms); Código de estado 201; Usuario registrado con campos clave | Pasa |
| API-03 | Registro sin campos | `POST api_aforo/auth/registro.php` | HTTP 400; Código de estado 400; Respuesta con success false y mensaje de validación | HTTP 400 (3 ms); Código de estado 400; Respuesta con success false y mensaje de validación | Pasa |
| API-04 | Login OK | `POST api_aforo/auth/login.php` | HTTP 200; Código de estado 200; Inicio de sesión exitoso con token y datos de usuario | HTTP 200 (53 ms); Código de estado 200; Inicio de sesión exitoso con token y datos de usuario | Pasa |
| API-05 | Login con contraseña errónea | `POST api_aforo/auth/login.php` | HTTP 401; Código de estado 401; Credenciales inválidas devuelven success false sin token | HTTP 401 (42 ms); Código de estado 401; Credenciales inválidas devuelven success false sin token | Pasa |

### Carpeta: ESP32

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-06 | Medición OK | `POST api_aforo/esp32/medicion.php` | HTTP 200; Código de estado 200; Medición registrada con campos clave | HTTP 200 (9 ms); Código de estado 200; Medición registrada con campos clave | Pasa |
| API-07 | Medición sin api_key | `POST api_aforo/esp32/medicion.php` | HTTP 401; Código de estado 401; Respuesta con success false por falta de api_key | HTTP 401 (3 ms); Código de estado 401; Respuesta con success false por falta de api_key | Pasa |
| API-08 | Medición con api_key inválida | `POST api_aforo/esp32/medicion.php` | HTTP 401; Código de estado 401; Respuesta con success false por dispositivo no autorizado | HTTP 401 (3 ms); Código de estado 401; Respuesta con success false por dispositivo no autorizado | Pasa |
| API-09 | Medición con valores no numéricos | `POST api_aforo/esp32/medicion.php` | HTTP 400; Código de estado 400; Respuesta con success false por valores no numéricos | HTTP 400 (3 ms); Código de estado 400; Respuesta con success false por valores no numéricos | Pasa |
| API-10 | Acceso Ana 1a vez (Entrada) | `POST api_aforo/esp32/acceso.php` | HTTP 200; Código de estado 200; Entrada registrada para Ana y aforo actualizado | HTTP 200 (9 ms); Código de estado 200; Entrada registrada para Ana y aforo actualizado | Pasa |
| API-11 | Acceso Ana 2a vez (Salida) | `POST api_aforo/esp32/acceso.php` | HTTP 200; Código de estado 200; Salida registrada para Ana tras la entrada previa | HTTP 200 (5 ms); Código de estado 200; Salida registrada para Ana tras la entrada previa | Pasa |
| API-12 | Acceso tarjeta desconocida | `POST api_aforo/esp32/acceso.php` | HTTP 200; Código de estado 200; Tarjeta no registrada devuelve success false y error tarjeta_no_registrada | HTTP 200 (4 ms); Código de estado 200; Tarjeta no registrada devuelve success false y error tarjeta_no_registrada | Pasa |

### Carpeta: App

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-13 | Login (obtener token) | `POST api_aforo/auth/login.php` | HTTP 200; Código de estado 200; Login exitoso y guarda token para endpoints de App | HTTP 200 (41 ms); Código de estado 200; Login exitoso y guarda token para endpoints de App | Pasa |
| API-14 | Estado OK | `GET api_aforo/app/estado.php?dispositivo_id=1` | HTTP 200; Código de estado 200; Estado del dispositivo con campos planos esperados | HTTP 200 (1 ms); Código de estado 200; Estado del dispositivo con campos planos esperados | Pasa |
| API-15 | Estado sin token | `GET api_aforo/app/estado.php?dispositivo_id=1` | HTTP 401; Código de estado 401; Rechaza petición sin X-Token | HTTP 401 (1 ms); Código de estado 401; Rechaza petición sin X-Token | Pasa |
| API-16 | Estado con token inválido | `GET api_aforo/app/estado.php?dispositivo_id=1` | HTTP 401; Código de estado 401; Rechaza petición con X-Token inválido | HTTP 401 (1 ms); Código de estado 401; Rechaza petición con X-Token inválido | Pasa |
| API-17 | Dentro OK | `GET api_aforo/app/dentro.php?dispositivo_id=1` | HTTP 200; Código de estado 200; Lista de personas dentro con total y campos esperados | HTTP 200 (2 ms); Código de estado 200; Lista de personas dentro con total y campos esperados | Pasa |
| API-18 | Historial accesos OK | `GET api_aforo/app/historial_accesos.php?dispositivo_id=1&limite=50` | HTTP 200; Código de estado 200; Historial de accesos con registros, total y campos esperados | HTTP 200 (2 ms); Código de estado 200; Historial de accesos con registros, total y campos esperados | Pasa |
| API-19 | Historial mediciones OK (orden asc) | `GET api_aforo/app/historial_mediciones.php?dispositivo_id=1&orden=asc&limite=50` | HTTP 200; Código de estado 200; Historial de mediciones en orden ascendente con campos esperados | HTTP 200 (2 ms); Código de estado 200; Historial de mediciones en orden ascendente con campos esperados | Pasa |
| API-20 | Historial fecha inválida 400 | `GET api_aforo/app/historial_accesos.php?desde=2026-02-30` | HTTP 400; Código de estado 400; Rechaza fecha inválida con success false | HTTP 400 (1 ms); Código de estado 400; Rechaza fecha inválida con success false | Pasa |

### Carpeta: Gestion

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-21 | Listar dispositivos | `GET api_aforo/app/dispositivos.php` | HTTP 200; Código de estado 200; Lista de dispositivos con campos esperados | HTTP 200 (2 ms); Código de estado 200; Lista de dispositivos con campos esperados | Pasa |
| API-22 | Crear dispositivo | `POST api_aforo/app/dispositivos.php` | HTTP 201; Código de estado 201; Dispositivo creado con api_key generada | HTTP 201 (14 ms); Código de estado 201; Dispositivo creado con api_key generada | Pasa |
| API-23 | Editar dispositivo | `POST api_aforo/app/dispositivos.php` | HTTP 200; Código de estado 200; Dispositivo actualizado correctamente | HTTP 200 (6 ms); Código de estado 200; Dispositivo actualizado correctamente | Pasa |
| API-24 | Desactivar dispositivo | `POST api_aforo/app/dispositivos.php` | HTTP 200; Código de estado 200; Dispositivo marcado como inactivo | HTTP 200 (6 ms); Código de estado 200; Dispositivo marcado como inactivo | Pasa |
| API-25 | Activar dispositivo | `POST api_aforo/app/dispositivos.php` | HTTP 200; Código de estado 200; Dispositivo reactivado correctamente | HTTP 200 (5 ms); Código de estado 200; Dispositivo reactivado correctamente | Pasa |
| API-26 | Listar personas y pendientes | `GET api_aforo/app/personas.php` | HTTP 200; Código de estado 200; Devuelve listas de personas y tarjetas pendientes | HTTP 200 (3 ms); Código de estado 200; Devuelve listas de personas y tarjetas pendientes | Pasa |
| API-27 | Crear persona | `POST api_aforo/app/personas.php` | HTTP 201; Código de estado 201; Persona creada correctamente | HTTP 201 (7 ms); Código de estado 201; Persona creada correctamente | Pasa |
| API-28 | Editar persona | `POST api_aforo/app/personas.php` | HTTP 200; Código de estado 200; Nombre de persona actualizado | HTTP 200 (7 ms); Código de estado 200; Nombre de persona actualizado | Pasa |
| API-29 | Asignar tarjeta pendiente | `POST api_aforo/app/personas.php` | HTTP 200; Código de estado 200; Tarjeta asignada en mayúsculas | HTTP 200 (5 ms); Código de estado 200; Tarjeta asignada en mayúsculas | Pasa |
| API-30 | Desactivar persona | `POST api_aforo/app/personas.php` | HTTP 200; Código de estado 200; Persona marcada como inactiva | HTTP 200 (6 ms); Código de estado 200; Persona marcada como inactiva | Pasa |
| API-31 | Activar persona | `POST api_aforo/app/personas.php` | HTTP 200; Código de estado 200; Persona reactivada correctamente | HTTP 200 (8 ms); Código de estado 200; Persona reactivada correctamente | Pasa |

### Carpeta: S3 Límites y alertas

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-32 | S3 Crear dispositivo propio | `POST api_aforo/app/dispositivos.php` | HTTP 201; S3 Crear dispositivo propio | HTTP 201 (7 ms); S3 Crear dispositivo propio | Pasa |
| API-33 | S3 Crear persona 1 | `POST api_aforo/app/personas.php` | HTTP 201; S3 Crear persona 1 | HTTP 201 (7 ms); S3 Crear persona 1 | Pasa |
| API-34 | S3 Crear persona 2 | `POST api_aforo/app/personas.php` | HTTP 201; S3 Crear persona 2 | HTTP 201 (7 ms); S3 Crear persona 2 | Pasa |
| API-35 | S3 Guardar límites POST | `POST api_aforo/app/limites.php` | HTTP 200; S3 Guardar límites POST | HTTP 200 (7 ms); S3 Guardar límites POST | Pasa |
| API-36 | S3 Guardar límites PUT extremos | `PUT api_aforo/app/limites.php` | HTTP 200; S3 Guardar límites PUT extremos | HTTP 200 (6 ms); S3 Guardar límites PUT extremos | Pasa |
| API-37 | S3 Restablecer límites de prueba | `PUT api_aforo/app/limites.php` | HTTP 200; S3 Restablecer límites de prueba | HTTP 200 (6 ms); S3 Restablecer límites de prueba | Pasa |
| API-38 | S3 Consultar límites | `GET api_aforo/app/limites.php?dispositivo_id=3` | HTTP 200; S3 Consultar límites | HTTP 200 (4 ms); S3 Consultar límites | Pasa |
| API-39 | S3 Rechazar temp_max -1 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar temp_max -1 | HTTP 400 (1 ms); S3 Rechazar temp_max -1 | Pasa |
| API-40 | S3 Rechazar temp_max 61 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar temp_max 61 | HTTP 400 (3 ms); S3 Rechazar temp_max 61 | Pasa |
| API-41 | S3 Rechazar hum_max -1 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar hum_max -1 | HTTP 400 (3 ms); S3 Rechazar hum_max -1 | Pasa |
| API-42 | S3 Rechazar hum_max 101 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar hum_max 101 | HTTP 400 (2 ms); S3 Rechazar hum_max 101 | Pasa |
| API-43 | S3 Rechazar aforo_max 0 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar aforo_max 0 | HTTP 400 (3 ms); S3 Rechazar aforo_max 0 | Pasa |
| API-44 | S3 Rechazar aforo_max 1.5 | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar aforo_max 1.5 | HTTP 400 (2 ms); S3 Rechazar aforo_max 1.5 | Pasa |
| API-45 | S3 Rechazar aforo_max True | `POST api_aforo/app/limites.php` | HTTP 400; S3 Rechazar aforo_max True | HTTP 400 (3 ms); S3 Rechazar aforo_max True | Pasa |
| API-46 | S3 Límites sin token | `GET api_aforo/app/limites.php?dispositivo_id=3` | HTTP 401; S3 Límites sin token | HTTP 401 (2 ms); S3 Límites sin token | Pasa |
| API-47 | S3 Alertas sin token | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=0&limite=10` | HTTP 401; S3 Alertas sin token | HTTP 401 (2 ms); S3 Alertas sin token | Pasa |
| API-48 | S3 Filtro inválido atendida=2 | `GET api_aforo/app/alertas.php?atendida=2` | HTTP 400; S3 Filtro inválido atendida=2 | HTTP 400 (3 ms); S3 Filtro inválido atendida=2 | Pasa |
| API-49 | S3 Filtro inválido limite=0 | `GET api_aforo/app/alertas.php?limite=0` | HTTP 400; S3 Filtro inválido limite=0 | HTTP 400 (2 ms); S3 Filtro inválido limite=0 | Pasa |
| API-50 | S3 Filtro inválido limit=0 | `GET api_aforo/app/alertas.php?limit=0` | HTTP 400; S3 Filtro inválido limit=0 | HTTP 400 (2 ms); S3 Filtro inválido limit=0 | Pasa |
| API-51 | S3 Alerta id inválido | `POST api_aforo/app/alertas.php` | HTTP 400; S3 Alerta id inválido | HTTP 400 (2 ms); S3 Alerta id inválido | Pasa |
| API-52 | S3 Medición en el límite | `POST api_aforo/esp32/medicion.php` | HTTP 200; S3 Medición en el límite | HTTP 200 (14 ms); S3 Medición en el límite | Pasa |
| API-53 | S3 Sin alertas en igualdad | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=0&limite=10` | HTTP 200; S3 Sin alertas en igualdad | HTTP 200 (4 ms); S3 Sin alertas en igualdad | Pasa |
| API-54 | S3 Temperatura alta | `POST api_aforo/esp32/medicion.php` | HTTP 200; S3 Temperatura alta | HTTP 200 (7 ms); S3 Temperatura alta | Pasa |
| API-55 | S3 Humedad alta | `POST api_aforo/esp32/medicion.php` | HTTP 200; S3 Humedad alta | HTTP 200 (7 ms); S3 Humedad alta | Pasa |
| API-56 | S3 Repetir ambas alertas | `POST api_aforo/esp32/medicion.php` | HTTP 200; S3 Repetir ambas alertas | HTTP 200 (6 ms); S3 Repetir ambas alertas | Pasa |
| API-57 | S3 Sin duplicados abiertos | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=0&limite=10` | HTTP 200; S3 Sin duplicados abiertos | HTTP 200 (3 ms); S3 Sin duplicados abiertos | Pasa |
| API-58 | S3 Atender alerta | `POST api_aforo/app/alertas.php` | HTTP 200; S3 Atender alerta | HTTP 200 (5 ms); S3 Atender alerta | Pasa |
| API-59 | S3 Atender alerta otra vez | `POST api_aforo/app/alertas.php` | HTTP 200; S3 Atender alerta otra vez | HTTP 200 (2 ms); S3 Atender alerta otra vez | Pasa |
| API-60 | S3 Filtrar atendidas y limit | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=1&limit=1` | HTTP 200; S3 Filtrar atendidas y limit | HTTP 200 (3 ms); S3 Filtrar atendidas y limit | Pasa |
| API-61 | S3 Renovar alerta atendida | `POST api_aforo/esp32/medicion.php` | HTTP 200; S3 Renovar alerta atendida | HTTP 200 (7 ms); S3 Renovar alerta atendida | Pasa |
| API-62 | S3 Nueva alerta abierta tras atender | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=0&limite=10` | HTTP 200; S3 Nueva alerta abierta tras atender | HTTP 200 (4 ms); S3 Nueva alerta abierta tras atender | Pasa |
| API-63 | S3 Entrada persona 1 | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Entrada persona 1 | HTTP 200 (6 ms); S3 Entrada persona 1 | Pasa |
| API-64 | S3 Rechazar entrada llena | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Rechazar entrada llena | HTTP 200 (7 ms); S3 Rechazar entrada llena | Pasa |
| API-65 | S3 Repetir rechazo lleno | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Repetir rechazo lleno | HTTP 200 (3 ms); S3 Repetir rechazo lleno | Pasa |
| API-66 | S3 Una sola alerta de aforo | `GET api_aforo/app/alertas.php?dispositivo_id=3&atendida=0&limite=10` | HTTP 200; S3 Una sola alerta de aforo | HTTP 200 (2 ms); S3 Una sola alerta de aforo | Pasa |
| API-67 | S3 Salida permitida con aforo lleno | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Salida permitida con aforo lleno | HTTP 200 (6 ms); S3 Salida permitida con aforo lleno | Pasa |
| API-68 | S3 Entrada rechazada no dejó registro | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Entrada rechazada no dejó registro | HTTP 200 (7 ms); S3 Entrada rechazada no dejó registro | Pasa |
| API-69 | S3 Salida persona 2 | `POST api_aforo/esp32/acceso.php` | HTTP 200; S3 Salida persona 2 | HTTP 200 (6 ms); S3 Salida persona 2 | Pasa |

### Carpeta: S3 Comandos

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-70 | S3 Identificar dispositivo seed para comandos | `GET api_aforo/app/dispositivos.php` | HTTP 200; S3 Identificar dispositivo seed para comandos | HTTP 200 (2 ms); S3 Identificar dispositivo seed para comandos | Pasa |
| API-71 | S3 Comando sin token | `POST api_aforo/app/comando.php` | HTTP 401; S3 Comando sin token | HTTP 401 (1 ms); S3 Comando sin token | Pasa |
| API-72 | S3 Rechazar acción de comando inválida | `POST api_aforo/app/comando.php` | HTTP 400; S3 Rechazar acción de comando inválida | HTTP 400 (2 ms); S3 Rechazar acción de comando inválida | Pasa |
| API-73 | S3 Comando con api_key inválida | `GET api_aforo/esp32/comando.php?api_key=clave-invalida` | HTTP 401; S3 Comando con api_key inválida | HTTP 401 (1 ms); S3 Comando con api_key inválida | Pasa |
| API-74 | S3 Vaciar comandos pendientes del seed | `GET api_aforo/esp32/comando.php?api_key=demo-esp32-key-0001` | HTTP 200; S3 Vaciar comandos pendientes del seed | HTTP 200 (7 ms); S3 Vaciar comandos pendientes del seed | Pasa |
| API-75 | S3 Encolar abrir_acceso pendiente | `POST api_aforo/app/comando.php` | HTTP 201; S3 Encolar abrir_acceso pendiente | HTTP 201 (6 ms); S3 Encolar abrir_acceso pendiente | Pasa |
| API-76 | S3 Entregar comando como ejecutado | `GET api_aforo/esp32/comando.php?api_key=demo-esp32-key-0001` | HTTP 200; S3 Entregar comando como ejecutado | HTTP 200 (8 ms); S3 Entregar comando como ejecutado | Pasa |
| API-77 | S3 Segunda consulta sin comando pendiente | `GET api_aforo/esp32/comando.php?api_key=demo-esp32-key-0001` | HTTP 200; S3 Segunda consulta sin comando pendiente | HTTP 200 (3 ms); S3 Segunda consulta sin comando pendiente | Pasa |
| API-78 | S3 Listar comando ejecutado por dispositivo | `GET api_aforo/app/comando.php?dispositivo_id=1` | HTTP 200; S3 Listar comando ejecutado por dispositivo | HTTP 200 (2 ms); S3 Listar comando ejecutado por dispositivo | Pasa |

### Carpeta: Exportar

| ID | Caso de prueba | Petición | Esperado | Obtenido | Resultado |
| --- | --- | --- | --- | --- | --- |
| API-79 | Exportar accesos CSV | `GET api_aforo/app/exportar.php?tipo=accesos` | HTTP 200; Código de estado 200; Cabecera Content-Type contiene text/csv; Cuerpo inicia con BOM o contiene cabecera fecha | HTTP 200 (4 ms); Código de estado 200; Cabecera Content-Type contiene text/csv; Cuerpo inicia con BOM o contiene cabecera fecha | Pasa |
| API-80 | Exportar mediciones CSV | `GET api_aforo/app/exportar.php?tipo=mediciones` | HTTP 200; Código de estado 200; Cabecera Content-Type contiene text/csv | HTTP 200 (3 ms); Código de estado 200; Cabecera Content-Type contiene text/csv | Pasa |
| API-81 | Exportar tipo inválido | `GET api_aforo/app/exportar.php?tipo=invalido` | HTTP 400; Código de estado 400; Rechaza tipo inválido con success false | HTTP 400 (4 ms); Código de estado 400; Rechaza tipo inválido con success false | Pasa |
| API-82 | Exportar sin token | `GET api_aforo/app/exportar.php?tipo=accesos` | HTTP 401; Código de estado 401; Rechaza petición sin X-Token | HTTP 401 (3 ms); Código de estado 401; Rechaza petición sin X-Token | Pasa |

---

## 4. Cobertura por requerimiento y casos de seguridad

### Mapeo con requerimientos funcionales (RF01 a RF08)
Conforme al documento de especificación técnica (`docs/expediente/02-requerimientos-casos-uso.md`), el sistema formaliza los requerimientos funcionales RF01 a RF08. A continuación se detalla su cobertura efectiva en la suite de pruebas automatizadas:

| Requerimiento | Descripción | Peticiones que lo validan | Cobertura |
| --- | --- | --- | --- |
| **RF01** | Registro, autenticación y gestión de sesión de usuarios | `API-02` (Registro OK), `API-03` (Registro sin campos), `API-04` (Login OK), `API-05` (Login con contraseña errónea), `API-13` (Login obtener token) | Completa |
| **RF02** | Administración de dispositivos ESP32, personas y tarjetas pendientes | `API-21` (Listar dispositivos), `API-22` (Crear dispositivo), `API-23` (Editar dispositivo), `API-24` (Desactivar dispositivo), `API-25` (Activar dispositivo), `API-26` (Listar personas y pendientes), `API-27` (Crear persona), `API-28` (Editar persona), `API-29` (Asignar tarjeta pendiente), `API-30` (Desactivar persona), `API-31` (Activar persona), `API-32` a `API-34` (Creación de dispositivo y personas de prueba), `API-70` (Identificar dispositivo seed) | Completa |
| **RF03** | Adquisición de lecturas RFID y mediciones ambientales en el nodo IoT | `API-06` (Medición OK), `API-07` (Medición sin api_key), `API-08` (Medición con api_key inválida), `API-09` (Medición con valores no numéricos), `API-10` (Acceso Ana 1a vez Entrada), `API-11` (Acceso Ana 2a vez Salida), `API-12` (Acceso tarjeta desconocida), `API-52` (Medición en el límite), `API-54` (Temperatura alta), `API-55` (Humedad alta), `API-56` (Repetir ambas alertas), `API-61` (Renovar alerta atendida) | Completa |
| **RF04** | Control de acceso con alternancia entrada/salida y validación de aforo máximo | `API-10` (Acceso Ana 1a vez Entrada), `API-11` (Acceso Ana 2a vez Salida), `API-12` (Acceso tarjeta desconocida), `API-63` (Entrada persona 1), `API-64` (Rechazar entrada llena), `API-65` (Repetir rechazo lleno), `API-67` (Salida permitida con aforo lleno), `API-68` (Entrada rechazada no dejó registro), `API-69` (Salida persona 2) | Completa |
| **RF05** | Monitoreo en tiempo real en el Dashboard móvil | `API-14` (Estado OK), `API-17` (Dentro OK) | Completa |
| **RF06** | Configuración de límites y gestión de alertas con notificaciones | `API-35` a `API-37` (Guardar y consultar límites), `API-38` (Consultar límites), `API-39` a `API-45` (Validaciones de rangos y tipos en límites), `API-46` (Límites sin token), `API-47` a `API-51` (Alertas sin token y filtros inválidos), `API-53` (Sin alertas en igualdad), `API-57` (Sin duplicados abiertos), `API-58` y `API-59` (Atender alertas), `API-60` (Filtrar atendidas y limit), `API-62` (Nueva alerta abierta tras atender), `API-66` (Una sola alerta de aforo) | Completa |
| **RF07** | Control remoto bidireccional hacia el ESP32 (`abrir_acceso` y `alarma`) | `API-71` (Comando sin token), `API-72` (Rechazar acción inválida), `API-73` (Comando con api_key inválida), `API-74` (Vaciar comandos pendientes del seed), `API-75` (Encolar abrir_acceso pendiente), `API-76` (Entregar comando como ejecutado), `API-77` (Segunda consulta sin comando pendiente), `API-78` (Listar comando ejecutado por dispositivo) | Completa |
| **RF08** | Consulta de historial por fechas, gráficas de 24 h y exportación a CSV | `API-18` (Historial accesos OK), `API-19` (Historial mediciones OK), `API-20` (Historial fecha inválida 400), `API-79` (Exportar accesos CSV), `API-80` (Exportar mediciones CSV), `API-81` (Exportar tipo inválido), `API-82` (Exportar sin token) | Completa |
| **Diagnóstico** | Verificación de salud de la API (no es un RF del documento de especificación) | `API-01` (Estado de la API `index.php` con diagnóstico y conectividad) | Verificado |

### Casos de seguridad y pruebas negativas

La colección valida sistemáticamente el rechazo de peticiones no autorizadas, malformadas o en conflicto:
1. **Peticiones sin autenticación (HTTP 401 sin token):**
   - `API-15`: Estado sin `X-Token`.
   - `API-46`: Consulta de límites sin `X-Token`.
   - `API-47`: Consulta de alertas sin `X-Token`.
   - `API-71`: Encolado de comandos remotos sin `X-Token`.
   - `API-82`: Exportación a CSV sin `X-Token`.
2. **Autenticación con credenciales o token inválido (HTTP 401):**
   - `API-05`: Login con contraseña errónea.
   - `API-16`: Estado con `X-Token` alterado o inexistente.
3. **Acceso IoT sin autorización de dispositivo (HTTP 401 api_key):**
   - `API-07`: Medición sin parámetro `api_key`.
   - `API-08`: Medición con `api_key` inválida o no registrada.
   - `API-73`: Consulta de comandos con `api_key` inválida.
4. **Validaciones de datos de entrada y errores de cliente (HTTP 400):**
   - `API-03`: Registro sin campos obligatorios.
   - `API-09`: Medición con valores de temperatura o humedad no numéricos.
   - `API-20`: Historial con formato de fecha inválido (`2026-02-30`).
   - `API-39` a `API-45`: Rechazo de umbrales fuera de rango físico (`temp_max < 0`, `temp_max > 60`, `hum_max < 0`, `hum_max > 100`, `aforo_max <= 0` o tipos decimales/booleanos).
   - `API-48` a `API-51`: Filtros fuera de dominio (`atendida=2`, `limite=0`, `limit=0`, ID de alerta inválido).
   - `API-72`: Comando con acción desconocida o no soportada.
   - `API-81`: Exportación con parámetro `tipo` distinto de `accesos` o `mediciones`.
5. **Control de reglas de negocio y respuestas de integridad:**
   - `API-12`: Lectura de tarjeta desconocida (HTTP 200 con `success: false` y código `tarjeta_no_registrada`, insertando la tarjeta en `tarjetas_pendientes`).
   - `API-64` y `API-65`: Intento de entrada con aforo lleno (HTTP 200 con `success: false` y código `aforo_lleno`, sin registrar acceso).
   - `API-67`: Garantía de salida libre aunque el aforo esté lleno.
   - `API-68`: Confirmación de que el intento denegado por aforo lleno no dejó un registro fantasma en `registros_acceso`.

---

## 5. Hallazgos y correcciones durante el proyecto

Durante la integración y ejecución de pruebas del backend se identificaron y solventaron defectos clave mediante revisiones en ramas de desarrollo e integración a `origin/develop`:
- **Reasignación de tarjeta RFID de una persona inactiva (Pull Request #72, commit `c13a828`):**
  - *Problema:* Al intentar reutilizar una tarjeta RFID previamente asignada a una persona dada de baja lógica (`activo = 0`), el backend fallaba con conflicto HTTP 409 (`La tarjeta ya pertenece a otra persona`), impidiendo transferir credenciales físicas a nuevos usuarios.
  - *Corrección:* Se implementó la función `liberarTarjetaSiInactiva()` en `backend/api_aforo/app/personas.php`. Dentro de una transacción con bloqueo `FOR UPDATE`, si la tarjeta pertenece a una persona inactiva, su campo `uid_rfid` se sustituye por un marcador único (`SIN-<hex>`), permitiendo la reasignación inmediata sin colisiones de clave única.
- **Protección contra inyección de fórmulas en CSV y compatibilidad UTF-8 (Pull Request #97, commit `7d233be`):**
  - *Problema:* En la exportación de reportes (`backend/api_aforo/app/exportar.php`), si un nombre de usuario contenía caracteres de control o símbolos aritméticos al inicio (`=`, `+`, `-`, `@`, `\t`, `\r`), hojas de cálculo como Excel interpretaban el contenido como fórmulas ejecutables. Además, caracteres con acentos o eñes presentaban corrupción de visualización al carecer de marca de orden de bytes.
  - *Corrección:* Se añadió la función sanitizadora `celdaSegura()` para anteponer una comilla simple (`'`) a cadenas vulnerables y se incorporó la emisión obligatoria de la cabecera UTF-8 BOM (`\xEF\xBB\xBF`) al flujo `php://output`.
- **Entrega atómica y única en la cola de comandos del ESP32 (Pull Request #69, commit `7bef202`):**
  - *Problema:* Peticiones concurrentes o de sondeo continuo desde el ESP32 podían recuperar repetidamente el mismo comando antes de que su estado fuera actualizado, ejecutando múltiples veces acciones críticas como `abrir_acceso`.
  - *Corrección:* Se implementó la selección y marcado de estado a `ejecutado` (`fecha_ejecucion = NOW()`) en el endpoint `backend/api_aforo/esp32/comando.php`, garantizando que cada comando se despache una sola vez y que consultas subsiguientes devuelvan `comando: null`.
- **Idempotencia de alertas ambientales y rechazo seguro por aforo (commit `bc972a4`):**
  - *Problema:* Lecturas periódicas de sensores fuera de rango saturaban la base de datos con alertas abiertas idénticas. Además, las entradas denegadas por límite de aforo no debían corromper el cálculo de personas dentro ni bloquear a quienes intentaban salir.
  - *Corrección:* Se agregó verificación de existencia de alertas abiertas (`atendida = 0`) para el mismo dispositivo y tipo antes de insertar. En `esp32/acceso.php`, se garantizó que salidas sean siempre procesadas y que el rechazo por aforo lleno genere alerta sin crear registros espurios en `registros_acceso`.
- **Validación rigurosa de límites por dispositivo (commit `8dce55d`):**
  - *Problema:* No se delimitaban estrictamente los valores numéricos permitidos para umbrales operativos, exponiendo el sistema a valores negativos o física/lógicamente imposibles.
  - *Corrección:* Se integraron validaciones en `app/limites.php` para acotar temperatura entre 0 y 60 °C, humedad entre 0 y 100 %, y aforo como entero estrictamente mayor a 0, respondiendo con HTTP 400 ante transgresiones.

---

## 6. Limitaciones de la ronda de pruebas

- **Pruebas en red local:** La totalidad de las pruebas se ejecutó en entorno de red local y mediante el servidor embebido de PHP (`php -S 0.0.0.0:8080`) con MariaDB local; no se evaluaron fluctuaciones de latencia, pérdidas de paquetes de redes móviles o condiciones de enlaces WAN.
- **Sin pruebas de carga y estrés:** Las pruebas verifican corrección funcional y cumplimiento de contratos de API, pero no evalúan comportamiento bajo alta concurrencia ni volumen masivo de peticiones por segundo (no se ejecutaron pruebas de estrés con herramientas como k6 o Apache JMeter).
- **Tokens sin expiración automática:** Los tokens generados en el inicio de sesión (`usuarios.token`) no disponen de mecanismo de expiración por tiempo de inactividad (TTL); la sesión permanece activa indefinidamente hasta que el usuario realiza un cierre de sesión manual o se sobrescribe el token al iniciar sesión nuevamente.
