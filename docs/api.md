# 5. Referencia de la API REST

**Proyecto:** Aforo IoT — Sistema de control de acceso con RFID y monitoreo ambiental  
**Ubicación del código:** `backend/api_aforo/`

---

## 1. Introducción

La API REST de **Aforo IoT** expone endpoints HTTP en PHP 8 conectados a MariaDB/MySQL mediante PDO. Atiende a la aplicación móvil Android y a los nodos IoT basados en ESP32.

### URLs base

| Entorno | URL base |
| --- | --- |
| Desarrollo local (misma máquina) | `http://localhost:8080/api_aforo/` |
| Emulador Android (AVD) | `http://10.0.2.2:8080/api_aforo/` |
| Teléfono físico o ESP32 en red local | `http://<IP-de-la-laptop>:8080/api_aforo/` |

### Formato de datos y respuestas

- **Codificación:** JSON en UTF-8 (`Content-Type: application/json; charset=utf-8`, serializado con `JSON_UNESCAPED_UNICODE`). La única excepción es `GET app/exportar.php`, que en caso de éxito responde `text/csv; charset=utf-8`.
- **Zona horaria:** configurable en `config.php` mediante `ZONA_HORARIA`; por defecto `America/Mexico_City`.
- **Estructura de respuesta JSON:** todos los endpoints JSON utilizan un objeto plano con `success` y `mensaje`, más las claves específicas de cada operación en el mismo nivel raíz (sin anidar dentro de una clave `"data"`):

```json
{
  "success": true,
  "mensaje": "Descripción del resultado",
  "clave_1": "valor",
  "clave_2": 123
}
```

- **Lectura del cuerpo de entrada:** en peticiones con cuerpo, el servidor lee `php://input` como JSON (con respaldo en `$_POST`). Si el cuerpo no es JSON válido y `$_POST` está vacío, responde `400` con `"Cuerpo JSON inválido"`.

### CORS y preflight `OPTIONS`

En todas las peticiones (`lib/respuesta.php`), el servidor envía las siguientes cabeceras:

- `Access-Control-Allow-Origin: *` (configurable con la constante `CORS_ORIGEN` en `config.php`)
- `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, X-Token, X-Api-Key`

Las peticiones `OPTIONS` (preflight CORS) reciben respuesta inmediata `204 No Content` sin cuerpo.

### Errores globales de infraestructura

Cualquier endpoint que requiera base de datos (`conexion.php`) puede devolver `500` antes de ejecutar su lógica si:

- Falta `config.php` o faltan constantes `DB_HOST`, `DB_NAME`, `DB_USER` o `DB_PASS`: `"Error de configuración del servidor"`.
- Falla la conexión PDO con MariaDB/MySQL: `"No se pudo conectar con la base de datos"`.

---

## 2. Autenticación

| Tipo de cliente | Endpoints | Mecanismo | Detalles de implementación |
| --- | --- | --- | --- |
| **Público** | `index.php`, `auth/registro.php`, `auth/login.php` | Ninguno | Acceso sin credenciales previas. |
| **App móvil** | `app/*.php` | Encabezado `X-Token` | Token hexadecimal de 64 caracteres generado en `POST auth/login.php` (`random_bytes(32)`) y guardado en `usuarios.token`. Cada nuevo login reemplaza el token anterior del usuario. **No hay expiración de token implementada** en `auth/validar_token.php`: la validación comprueba coincidencia exacta en base de datos. |
| **ESP32** | `esp32/acceso.php`, `esp32/medicion.php` | `api_key` (cuerpo JSON o encabezado `X-Api-Key`) | `obtenerApiKey()` lee primero el campo `api_key` del cuerpo JSON y, si está vacío, el encabezado `X-Api-Key`. El dispositivo debe existir en `dispositivos` con `activo = 1`. |
| **ESP32** | `esp32/comando.php` | `api_key` (query, encabezado o cuerpo JSON) | Al ser `GET`, busca `api_key` en este orden de prioridad: parámetro query `?api_key=...`, encabezado `X-Api-Key` y, por último, campo `api_key` en un cuerpo JSON. El dispositivo debe estar activo (`activo = 1`). |

Respuestas de autenticación fallida (`401 Unauthorized`):

- App móvil: `"Token inválido o ausente"`.
- ESP32 sin clave: `"Falta api_key del dispositivo"`.
- ESP32 con clave inexistente o inactiva: `"Dispositivo no autorizado o inactivo"`.

---

## 3. Códigos HTTP usados

| Código | Nombre | Significado en esta API |
| --- | --- | --- |
| `200` | OK | Operación exitosa (consultas, login, actualizaciones, entrega de comandos, exportación CSV). En `POST esp32/acceso.php` también se devuelve `200` con `"success": false` ante rechazos de negocio (`tarjeta_no_registrada`, `aforo_lleno`). |
| `201` | Created | Recurso creado correctamente (`auth/registro.php`, alta en `app/dispositivos.php`, alta en `app/personas.php`, encolado en `app/comando.php`). |
| `204` | No Content | Respuesta sin cuerpo para peticiones preflight `OPTIONS`. |
| `400` | Bad Request | JSON malformado, campos obligatorios ausentes, tipos o formatos inválidos, valores fuera de rango, acción desconocida o dispositivo inactivo al encolar un comando. |
| `401` | Unauthorized | Credenciales inválidas en login, `X-Token` ausente o inválido, o `api_key` ausente, inválida o de un dispositivo inactivo. |
| `404` | Not Found | El recurso solicitado no existe (dispositivo, persona, alerta o registro de límites). |
| `405` | Method Not Allowed | Método HTTP no admitido por el endpoint; incluye cabecera `Allow`. |
| `409` | Conflict | Conflicto de unicidad: correo ya registrado (`auth/registro.php`) o tarjeta `uid_rfid` ya asignada a otra persona activa (`app/personas.php`). |
| `500` | Internal Server Error | Error de configuración del servidor, fallo de conexión a base de datos o excepción interna SQL/inesperada. |

---

## 4. Tabla resumen de endpoints

| Método | Ruta | Quién lo usa | Descripción | RF |
| --- | --- | --- | --- | --- |
| `GET` | `index.php` | Público | Diagnóstico del servicio, versión y fecha del servidor | — |
| `POST` | `auth/registro.php` | Público | Registra un nuevo usuario para la aplicación móvil | RF01 |
| `POST` | `auth/login.php` | Público | Autentica credenciales y devuelve un nuevo `X-Token` | RF01 |
| `POST` | `esp32/acceso.php` | ESP32 | Registra entrada o salida alternada por UID RFID y valida aforo | RF03, RF04 |
| `POST` | `esp32/medicion.php` | ESP32 | Guarda temperatura y humedad y evalúa alertas contra límites | RF03, RF06 |
| `GET` | `esp32/comando.php` | ESP32 | Entrega atómicamente el comando pendiente más antiguo | RF07 |
| `GET` | `app/estado.php` | App | Consulta aforo actual, límite, última medición y estado en línea | RF05 |
| `GET` | `app/dentro.php` | App | Lista las personas actualmente dentro del recinto | RF05 |
| `GET` | `app/historial_accesos.php` | App | Consulta historial de entradas y salidas con filtros | RF08 |
| `GET` | `app/historial_mediciones.php` | App | Consulta historial de temperatura y humedad con filtros y orden | RF08 |
| `GET` | `app/exportar.php` | App | Exporta accesos o mediciones en formato CSV con BOM UTF-8 | RF08 |
| `GET` | `app/dispositivos.php` | App | Lista todos los dispositivos ESP32 con su estado `en_linea` | RF02 |
| `POST` | `app/dispositivos.php` | App | Crea, edita, desactiva o activa un dispositivo ESP32 | RF02 |
| `GET` | `app/personas.php` | App | Lista personas registradas y tarjetas RFID pendientes | RF02 |
| `POST` | `app/personas.php` | App | Crea, edita, desactiva, activa persona o asigna tarjeta pendiente | RF02 |
| `GET` | `app/limites.php` | App | Consulta límites (`temp_max`, `hum_max`, `aforo_max`) de un dispositivo | RF06 |
| `POST` / `PUT` | `app/limites.php` | App | Crea o actualiza límites de clima y aforo de un dispositivo | RF06 |
| `GET` | `app/alertas.php` | App | Lista alertas con filtros opcionales de atención, dispositivo y límite | RF06 |
| `POST` | `app/alertas.php` | App | Marca una alerta como atendida (`atendida = 1`) | RF06 |
| `GET` | `app/comando.php` | App | Lista comandos recientes (globales o de un dispositivo) | RF07 |
| `POST` | `app/comando.php` | App | Encola un comando remoto (`abrir_acceso`, `activar_alarma`, `alarma`) | RF07 |

---

## 5. Detalle de endpoints

### 5.1 `GET index.php`

- **Propósito:** verificar que la API está operativa, conocer su versión, si existe `config.php` y la hora del servidor.
- **Autenticación:** ninguna (público).
- **Parámetros:** ninguno.

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/index.php"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "API Aforo IoT operativa",
  "servicio": "api_aforo",
  "version": "0.1.0",
  "configurado": true,
  "fecha_servidor": "2026-11-24T18:00:00-06:00"
}
```

**Respuestas de error:**

- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, OPTIONS`).

---

### 5.2 `POST auth/registro.php`

- **Propósito:** crear una nueva cuenta de usuario con contraseña cifrada (`password_hash`).
- **Autenticación:** ninguna (público).

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `nombre` | string | Sí | No vacío tras `trim`. |
| `correo` | string | Sí | Se normaliza con `trim` y minúsculas; debe pasar `FILTER_VALIDATE_EMAIL` y ser único en `usuarios`. |
| `password` | string | Sí | Mínimo 6 caracteres (`strlen >= 6`). |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/auth/registro.php" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Usuario Prueba",
    "correo": "usuario@aforoiot.local",
    "password": "clave123"
  }'
```

**Respuesta exitosa (`201 Created`):**

```json
{
  "success": true,
  "mensaje": "Usuario registrado correctamente",
  "usuario_id": 2,
  "nombre": "Usuario Prueba",
  "correo": "usuario@aforoiot.local"
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"Faltan campos obligatorios: nombre, correo y password"`
- `400`: `"El correo electrónico no es válido"`
- `400`: `"La contraseña debe tener al menos 6 caracteres"`
- `405`: `"Método HTTP no permitido"`
- `409`: `"El correo ya está registrado"`
- `500`: `"Error interno al registrar usuario"`

---

### 5.3 `POST auth/login.php`

- **Propósito:** autenticar un usuario y generar un token de sesión aleatorio de 64 caracteres hexadecimales (reemplaza cualquier token previo).
- **Autenticación:** ninguna (público).

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `correo` | string | Sí | Se normaliza con `trim` y minúsculas; no vacío. |
| `password` | string | Sí | No vacío; verificado con `password_verify`. |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/auth/login.php" \
  -H "Content-Type: application/json" \
  -d '{
    "correo": "admin@aforoiot.local",
    "password": "admin123"
  }'
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Inicio de sesión exitoso",
  "token": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
  "nombre": "Administrador",
  "rol": "admin",
  "usuario_id": 1
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"Faltan campos obligatorios: correo y password"`
- `401`: `"Correo o contraseña incorrectos"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al iniciar sesión"`

---

### 5.4 `POST esp32/acceso.php`

- **Propósito:** registrar de forma alternada una `entrada` o `salida` para el UID RFID leído por un ESP32, actualizando `ultima_conexion` del dispositivo y aplicando las reglas de tarjeta desconocida y aforo máximo.
- **Autenticación:** `api_key` de dispositivo activo (en cuerpo JSON o encabezado `X-Api-Key`).

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `api_key` | string | Condicional | Obligatorio en el cuerpo si no se envía el encabezado `X-Api-Key`. |
| `uid` | string | Sí | No vacío; se normaliza con `trim` y mayúsculas. |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/esp32/acceso.php" \
  -H "Content-Type: application/json" \
  -d '{
    "api_key": "demo-esp32-key-0001",
    "uid": "A1B2C3D4"
  }'
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Entrada registrada",
  "tipo": "entrada",
  "nombre": "Ana López",
  "dentro": 1
}
```

*(En salida, `mensaje` es `"Salida registrada"` y `tipo` es `"salida"`.)*

**Rechazos de negocio (devuelven HTTP `200` con `success: false`):**

- Tarjeta no registrada (`error: "tarjeta_no_registrada"`):

```json
{
  "success": false,
  "mensaje": "Tarjeta no registrada",
  "error": "tarjeta_no_registrada",
  "uid": "FFFFFFFF"
}
```

- Aforo lleno al intentar entrar (`error: "aforo_lleno"`):

```json
{
  "success": false,
  "mensaje": "Aforo lleno, acceso denegado",
  "error": "aforo_lleno",
  "nombre": "Ana López",
  "dentro": 1
}
```

**Respuestas de error HTTP:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El UID de la tarjeta es obligatorio"`
- `401`: `"Falta api_key del dispositivo"`
- `401`: `"Dispositivo no autorizado o inactivo"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al procesar el acceso"`

---

### 5.5 `POST esp32/medicion.php`

- **Propósito:** almacenar una lectura de temperatura y humedad enviada por el ESP32, actualizar `ultima_conexion` y evaluar alertas contra los límites configurados del dispositivo.
- **Autenticación:** `api_key` de dispositivo activo (en cuerpo JSON o encabezado `X-Api-Key`).

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `api_key` | string | Condicional | Obligatorio en el cuerpo si no se envía el encabezado `X-Api-Key`. |
| `temperatura` | number | Sí | Obligatorio, numérico (`is_numeric`) y finito (`is_finite`). |
| `humedad` | number | Sí | Obligatorio, numérico (`is_numeric`) y finito (`is_finite`). |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/esp32/medicion.php" \
  -H "Content-Type: application/json" \
  -d '{
    "api_key": "demo-esp32-key-0001",
    "temperatura": 24.5,
    "humedad": 55.2
  }'
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Medición registrada",
  "medicion_id": 42,
  "dispositivo_id": 1,
  "temperatura": 24.5,
  "humedad": 55.2,
  "alerta": false
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"Temperatura y humedad son obligatorias y deben ser numéricas"`
- `400`: `"Valores de temperatura o humedad inválidos"`
- `401`: `"Falta api_key del dispositivo"`
- `401`: `"Dispositivo no autorizado o inactivo"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al guardar la medición"`

---

### 5.6 `GET esp32/comando.php`

- **Propósito:** consultar y entregar de manera atómica el comando pendiente más antiguo del dispositivo, marcándolo como `ejecutado` antes de responder. Actualiza `ultima_conexion`.
- **Autenticación:** `api_key` de dispositivo activo (query `?api_key=...`, encabezado `X-Api-Key` o campo `api_key` en cuerpo JSON).

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `api_key` | string | Condicional | Obligatorio si no se envía por `X-Api-Key` ni en cuerpo JSON. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/esp32/comando.php?api_key=demo-esp32-key-0001"
```

**Respuesta exitosa con comando (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Comando obtenido para ejecución",
  "comando": {
    "id": 5,
    "dispositivo_id": 1,
    "accion": "abrir_acceso",
    "estado": "ejecutado",
    "fecha_creacion": "2026-11-24 18:00:00",
    "fecha_ejecucion": "2026-11-24 18:00:02"
  }
}
```

**Respuesta exitosa sin comandos pendientes (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "No hay comandos pendientes",
  "comando": null
}
```

**Respuestas de error:**

- `401`: `"Falta api_key del dispositivo"`
- `401`: `"Dispositivo no autorizado o inactivo"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al consultar comandos"`

---

### 5.7 `GET app/estado.php`

- **Propósito:** obtener el estado actual de un dispositivo activo: conteo `dentro`, `aforo_max`, última medición ambiental, `ultima_conexion` e indicador `en_linea` (`ultima_conexion` dentro de los últimos 30 segundos).
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). Si se omite, selecciona el primer dispositivo con `activo = 1` ordenado por `id ASC`. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/estado.php?dispositivo_id=1" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Estado del dispositivo obtenido",
  "dispositivo_id": 1,
  "nombre_dispositivo": "ESP32 Entrada",
  "dentro": 2,
  "aforo_max": 30,
  "medicion": {
    "temperatura": 24.5,
    "humedad": 55.2,
    "fecha": "2026-11-24 18:05:00"
  },
  "en_linea": true,
  "ultima_conexion": "2026-11-24 18:05:10"
}
```

*(Si no hay mediciones o límites registrados, `medicion`, `aforo_max` o `ultima_conexion` se devuelven como `null`.)*

**Respuestas de error:**

- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al consultar el estado"`

---

### 5.8 `GET app/dentro.php`

- **Propósito:** listar las personas que se encuentran actualmente dentro según la vista `personas_dentro`, ordenadas por `hora_entrada DESC, id DESC`.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). Si se omite, devuelve las personas dentro de todos los dispositivos. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/dentro.php?dispositivo_id=1" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Personas dentro obtenidas",
  "personas": [
    {
      "id": 1,
      "nombre": "Ana López",
      "hora_entrada": "2026-11-24 17:50:00"
    }
  ],
  "total": 1
}
```

**Respuestas de error:**

- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al consultar personas dentro"`

---

### 5.9 `GET app/historial_accesos.php`

- **Propósito:** consultar el historial de entradas y salidas (`registros_acceso` unido con `personas`), ordenado por `fecha DESC, id DESC`.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). |
| `desde` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida. Incluye desde `00:00:00`. |
| `hasta` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida; debe cumplir `desde <= hasta`. Incluye el día completo (`< hasta + 1 día 00:00:00`). |
| `limite` | int | No | Entero positivo. Por defecto `100`; se acota a un máximo de `1000`. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/historial_accesos.php?dispositivo_id=1&desde=2026-11-01&hasta=2026-11-24&limite=50" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Historial de accesos obtenido",
  "registros": [
    {
      "id": 10,
      "persona_id": 1,
      "nombre": "Ana López",
      "tipo": "entrada",
      "fecha": "2026-11-24 17:50:00",
      "dispositivo_id": 1
    }
  ],
  "total": 1
}
```

**Respuestas de error:**

- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `400`: `"El parámetro desde debe tener formato YYYY-MM-DD"` / `"El parámetro hasta debe tener formato YYYY-MM-DD"`
- `400`: `"El parámetro desde no es una fecha válida"` / `"El parámetro hasta no es una fecha válida"`
- `400`: `"El parámetro desde no puede ser mayor que hasta"`
- `400`: `"El parámetro limite debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al consultar el historial de accesos"`

---

### 5.10 `GET app/historial_mediciones.php`

- **Propósito:** consultar el historial de mediciones ambientales (`mediciones`) con soporte de ordenamiento ascendente o descendente para listados y gráficas.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). |
| `desde` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida. |
| `hasta` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida; debe cumplir `desde <= hasta`. |
| `limite` | int | No | Entero positivo. Por defecto `100`; máximo `1000`. |
| `orden` | string | No | `asc` o `desc` (insensible a mayúsculas). Por defecto `desc`. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/historial_mediciones.php?dispositivo_id=1&orden=asc&limite=50" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Historial de mediciones obtenido",
  "mediciones": [
    {
      "id": 42,
      "temperatura": 24.5,
      "humedad": 55.2,
      "fecha": "2026-11-24 18:05:00",
      "dispositivo_id": 1
    }
  ],
  "total": 1
}
```

**Respuestas de error:**

- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `400`: `"El parámetro desde debe tener formato YYYY-MM-DD"` / `"El parámetro hasta debe tener formato YYYY-MM-DD"`
- `400`: `"El parámetro desde no es una fecha válida"` / `"El parámetro hasta no es una fecha válida"`
- `400`: `"El parámetro desde no puede ser mayor que hasta"`
- `400`: `"El parámetro limite debe ser un entero positivo"`
- `400`: `"El parámetro orden debe ser asc o desc"`
- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al consultar el historial de mediciones"`

---

### 5.11 `GET app/exportar.php`

- **Propósito:** descargar un archivo CSV con el historial de accesos o mediciones filtrado por dispositivo y rango de fechas, ordenado cronológicamente (`fecha ASC, id ASC`).
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `tipo` | string | Sí | Debe ser exactamente `accesos` o `mediciones`. |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). |
| `desde` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida. |
| `hasta` | string | No | Formato estricto `YYYY-MM-DD` y fecha calendario válida; debe cumplir `desde <= hasta`. |

**Características de la respuesta CSV (`200 OK`):**

- **Content-Type:** `text/csv; charset=utf-8` con **BOM UTF-8** (`\xEF\xBB\xBF`) al inicio del archivo.
- **Nombre de archivo (`Content-Disposition: attachment`):** `<tipo>_<desde|todo>_<hasta|todo>.csv` (por ejemplo, `accesos_2026-11-01_2026-11-24.csv` o `mediciones_todo_todo.csv`).
- **Límite de filas:** máximo `10000` registros (`LIMITE_MAXIMO = 10000`).
- **Columnas para `tipo=accesos`:** `ID,Fecha,Nombre,Tipo,Dispositivo`.
- **Columnas para `tipo=mediciones`:** `ID,Fecha,Temperatura,Humedad,Dispositivo`.
- **Protección contra inyección de fórmulas:** en `tipo=accesos`, la función `celdaSegura()` antepone un apóstrofe (`'`) a cualquier valor de `Nombre` que comience con `=`, `+`, `-`, `@`, tabulador (`\t`) o retorno de carro (`\r`).

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/exportar.php?tipo=accesos&desde=2026-11-01&hasta=2026-11-24" \
  -H "X-Token: <token>" \
  -o accesos.csv
```

**Ejemplo de salida CSV (`200 OK`):**

```csv
ID,Fecha,Nombre,Tipo,Dispositivo
10,"2026-11-24 17:50:00","Ana López",entrada,1
```

**Respuestas de error (en JSON):**

- `400`: `"El parámetro tipo es obligatorio y debe ser accesos o mediciones"`
- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `400`: `"El parámetro desde debe tener formato YYYY-MM-DD"` / `"El parámetro hasta debe tener formato YYYY-MM-DD"`
- `400`: `"El parámetro desde no es una fecha válida"` / `"El parámetro hasta no es una fecha válida"`
- `400`: `"El parámetro desde no puede ser mayor que hasta"`
- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al generar el archivo CSV"` / `"Error interno al exportar los datos"`

---

### 5.12 `GET app/dispositivos.php`

- **Propósito:** listar todos los dispositivos registrados (activos e inactivos) ordenados por `id ASC`, incluyendo el cálculo `en_linea` (`ultima_conexion` en los últimos 30 segundos).
- **Autenticación:** encabezado `X-Token`.
- **Parámetros:** ninguno.

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/dispositivos.php" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Dispositivos obtenidos",
  "dispositivos": [
    {
      "id": 1,
      "nombre": "ESP32 Entrada",
      "ubicacion": "Puerta Principal",
      "api_key": "demo-esp32-key-0001",
      "activo": true,
      "ultima_conexion": "2026-11-24 18:05:10",
      "en_linea": true
    }
  ]
}
```

**Respuestas de error:**

- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, POST, OPTIONS`)
- `500`: `"Error interno al consultar los dispositivos"`

---

### 5.13 `POST app/dispositivos.php`

- **Propósito:** crear, editar, desactivar o activar un dispositivo ESP32 según el campo `accion`. Al crear un dispositivo se genera automáticamente su `api_key` (32 caracteres hexadecimales) y su fila inicial en `limites` dentro de una transacción.
- **Autenticación:** encabezado `X-Token`.

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `accion` | string | Sí | `crear`, `editar`, `desactivar` o `activar` (insensible a mayúsculas). |
| `id` | int | En `editar`, `desactivar`, `activar` | Entero positivo (`FILTER_VALIDATE_INT` y `> 0`). |
| `nombre` | string | En `crear`; opcional en `editar` | Obligatorio no vacío, máximo `100` caracteres (`mb_strlen`). En `editar` se requiere al menos `nombre` o `ubicacion`. |
| `ubicacion` | string \| null | No | Opcional en `crear` y `editar`; máximo `150` caracteres. Cadena vacía o `null` se almacena como `null`. |

**Ejemplo de request (`crear`):**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/dispositivos.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "crear",
    "nombre": "ESP32 Laboratorio",
    "ubicacion": "Edificio B"
  }'
```

**Ejemplo de request (`editar`):**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/dispositivos.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "editar",
    "id": 2,
    "nombre": "ESP32 Laboratorio Norte",
    "ubicacion": "Planta Alta"
  }'
```

**Ejemplo de request (`desactivar` / `activar`):**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/dispositivos.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "desactivar",
    "id": 2
  }'
```

**Respuesta exitosa (`201 Created` para `crear`; `200 OK` para `editar`, `activar`, `desactivar`):**

```json
{
  "success": true,
  "mensaje": "Dispositivo creado",
  "dispositivo": {
    "id": 2,
    "nombre": "ESP32 Laboratorio",
    "ubicacion": "Edificio B",
    "api_key": "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    "activo": true,
    "ultima_conexion": null,
    "en_linea": false
  }
}
```

*(Mensajes de éxito según acción: `"Dispositivo creado"`, `"Dispositivo actualizado"`, `"Dispositivo activado"`, `"Dispositivo desactivado"`.)*

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El nombre del dispositivo es obligatorio y máximo de 100 caracteres"`
- `400`: `"La ubicación debe tener máximo 150 caracteres"`
- `400`: `"El campo id debe ser un entero positivo"`
- `400`: `"Debe indicar al menos nombre o ubicacion para editar"`
- `400`: `"Acción no válida. Use crear, editar, desactivar o activar"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al gestionar el dispositivo"`

---

### 5.14 `GET app/personas.php`

- **Propósito:** obtener el catálogo completo de personas (`ORDER BY id ASC`) junto con las tarjetas RFID desconocidas en `tarjetas_pendientes` (`ORDER BY fecha DESC, id DESC`).
- **Autenticación:** encabezado `X-Token`.
- **Parámetros:** ninguno.

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/personas.php" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Personas y tarjetas pendientes obtenidas",
  "personas": [
    {
      "id": 1,
      "nombre": "Ana López",
      "uid_rfid": "A1B2C3D4",
      "activo": true
    }
  ],
  "pendientes": [
    {
      "uid_rfid": "FFFFFFFF",
      "dispositivo_id": 1,
      "fecha": "2026-11-24 18:00:00"
    }
  ]
}
```

**Respuestas de error:**

- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, POST, OPTIONS`)
- `500`: `"Error interno al consultar personas"`

---

### 5.15 `POST app/personas.php`

- **Propósito:** crear, editar, desactivar, activar personas o asignar una tarjeta RFID pendiente a una persona existente o nueva.
- **Autenticación:** encabezado `X-Token`.

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `accion` | string | Sí | `crear`, `editar`, `desactivar`, `activar` o `asignar` (insensible a mayúsculas). |
| `id` | int | En `editar`, `desactivar`, `activar` | Entero positivo. |
| `nombre` | string | En `crear`; condicional en `editar` y `asignar` | No vacío, máximo `100` caracteres (`mb_strlen`). En `editar` debe enviarse al menos `nombre` o `uid_rfid`. En `asignar` es obligatorio solo si no se envía `persona_id`. |
| `uid_rfid` | string | En `asignar`; opcional en `crear` y `editar` | Hexadecimal en mayúsculas de 4 a 32 caracteres (`/^[0-9A-F]{4,32}$/`). En `crear`, si se omite o viene vacío, se asigna un marcador temporal `SIN-<16-hex>`. |
| `persona_id` | int | Condicional en `asignar` | Entero positivo. Si se envía junto con `nombre`, tiene prioridad `persona_id` y actualiza a esa persona. |

**Ejemplos de request:**

```bash
# Crear persona sin tarjeta física asignada
curl -X POST "http://localhost:8080/api_aforo/app/personas.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "crear",
    "nombre": "Persona Demo"
  }'

# Editar persona
curl -X POST "http://localhost:8080/api_aforo/app/personas.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "editar",
    "id": 3,
    "nombre": "Persona Demo Editada"
  }'

# Asignar tarjeta pendiente a persona existente (elimina el UID de tarjetas_pendientes)
curl -X POST "http://localhost:8080/api_aforo/app/personas.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "asignar",
    "persona_id": 3,
    "uid_rfid": "ABCD1234"
  }'

# Desactivar / activar persona
curl -X POST "http://localhost:8080/api_aforo/app/personas.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "accion": "desactivar",
    "id": 3
  }'
```

**Respuesta exitosa (`201 Created` para `crear`; `200 OK` para las demás acciones):**

```json
{
  "success": true,
  "mensaje": "Tarjeta asignada correctamente",
  "persona": {
    "id": 3,
    "nombre": "Persona Demo Editada",
    "uid_rfid": "ABCD1234",
    "activo": true
  }
}
```

*(Mensajes de éxito según acción: `"Persona creada"`, `"Persona actualizada"`, `"Persona activada"`, `"Persona desactivada"`, `"Tarjeta asignada correctamente"`.)*

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El nombre de la persona es obligatorio y máximo de 100 caracteres"`
- `400`: `"El campo id debe ser un entero positivo"` / `"El campo persona_id debe ser un entero positivo"`
- `400`: `"El campo uid_rfid debe ser hexadecimal de 4 a 32 caracteres"`
- `400`: `"Debe indicar al menos nombre para editar"`
- `400`: `"Debe indicar persona_id o nombre para asignar la tarjeta"`
- `400`: `"Acción no válida. Use crear, editar, desactivar, activar o asignar"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Persona no encontrada"`
- `405`: `"Método HTTP no permitido"`
- `409`: `"La tarjeta ya pertenece a otra persona"`
- `500`: `"Error interno al gestionar personas"`

---

### 5.16 `GET app/limites.php`

- **Propósito:** consultar los umbrales de temperatura, humedad y aforo configurados para un dispositivo.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | Sí | Entero positivo (`FILTER_VALIDATE_INT` y `> 0`, no booleano). |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/limites.php?dispositivo_id=1" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Límites obtenidos",
  "limites": {
    "dispositivo_id": 1,
    "temp_max": 30,
    "hum_max": 75,
    "aforo_max": 30
  }
}
```

**Respuestas de error:**

- `400`: `"El campo dispositivo_id debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `404`: `"Límites no encontrados para el dispositivo"`
- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, POST, PUT, OPTIONS`)
- `500`: `"Error interno al gestionar los límites"`

---

### 5.17 `POST` / `PUT app/limites.php`

- **Propósito:** crear o actualizar los límites de clima y aforo de un dispositivo (`INSERT ... ON DUPLICATE KEY UPDATE`). `POST` y `PUT` ejecutan exactamente la misma lógica.
- **Autenticación:** encabezado `X-Token`.

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | Sí | Entero positivo (no booleano); debe existir en `dispositivos`. |
| `temp_max` | number | Sí | Numérico finito en el rango `0` a `60` °C (inclusive). |
| `hum_max` | number | Sí | Numérico finito en el rango `0` a `100` % (inclusive). |
| `aforo_max` | int | Sí | Entero positivo entre `1` y `4294967295` (no booleano ni decimal). |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/limites.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "dispositivo_id": 1,
    "temp_max": 30,
    "hum_max": 75,
    "aforo_max": 1
  }'
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Límites guardados",
  "limites": {
    "dispositivo_id": 1,
    "temp_max": 30,
    "hum_max": 75,
    "aforo_max": 1
  }
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El campo dispositivo_id debe ser un entero positivo"`
- `400`: `"El campo temp_max debe ser numérico entre 0 y 60 °C"`
- `400`: `"El campo hum_max debe ser numérico entre 0 y 100 %"`
- `400`: `"El campo aforo_max debe ser un entero positivo hasta 4294967295"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `404`: `"Límites no encontrados para el dispositivo"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al gestionar los límites"`

---

### 5.18 `GET app/alertas.php`

- **Propósito:** listar alertas registradas ordenadas por `fecha DESC, id DESC`, con filtros opcionales.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `atendida` | string | No | Exactamente `0` (pendientes) o `1` (atendidas). |
| `dispositivo_id` | int | No | Entero positivo (`FILTER_VALIDATE_INT` y `> 0`). |
| `limite` (o `limit`) | int | No | Entero positivo. Por defecto `100`; máximo `1000`. Si se envían ambos, tiene prioridad `limite`. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/alertas.php?dispositivo_id=1&atendida=0&limite=10" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Alertas obtenidas",
  "alertas": [
    {
      "id": 3,
      "dispositivo_id": 1,
      "tipo": "temperatura",
      "mensaje": "Temperatura por encima del límite",
      "valor": 31,
      "fecha": "2026-11-24 18:10:00",
      "atendida": false
    }
  ],
  "total": 1
}
```

**Respuestas de error:**

- `400`: `"El parámetro atendida debe ser 0 o 1"`
- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `400`: `"El parámetro limite debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, POST, OPTIONS`)
- `500`: `"Error interno al gestionar las alertas"`

---

### 5.19 `POST app/alertas.php`

- **Propósito:** marcar una alerta existente como atendida (`atendida = 1`). La operación es idempotente si la alerta ya estaba atendida.
- **Autenticación:** encabezado `X-Token`.

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `id` | int | Sí | Entero positivo (`FILTER_VALIDATE_INT` y `> 0`, no booleano). |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/alertas.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "id": 3
  }'
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Alerta marcada como atendida",
  "id": 3,
  "atendida": true
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El campo id debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Alerta no encontrada"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al gestionar las alertas"`

---

### 5.20 `GET app/comando.php`

- **Propósito:** listar los comandos remotos recientes (`ORDER BY fecha_creacion DESC, id DESC`), de todos los dispositivos o filtrados por uno específico.
- **Autenticación:** encabezado `X-Token`.

| Parámetro (query) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` | int | No | Entero positivo (`ctype_digit` y `> 0`). Si se envía, el dispositivo debe existir (`404` si no existe). |
| `limite` | int | No | Entero positivo. Por defecto `50`; máximo `100`. |

**Ejemplo de request:**

```bash
curl -X GET "http://localhost:8080/api_aforo/app/comando.php?dispositivo_id=1&limite=20" \
  -H "X-Token: <token>"
```

**Respuesta exitosa (`200 OK`):**

```json
{
  "success": true,
  "mensaje": "Comandos obtenidos",
  "comandos": [
    {
      "id": 5,
      "dispositivo_id": 1,
      "accion": "abrir_acceso",
      "estado": "ejecutado",
      "fecha_creacion": "2026-11-24 18:00:00",
      "fecha_ejecucion": "2026-11-24 18:00:02"
    }
  ],
  "total": 1
}
```

**Respuestas de error:**

- `400`: `"El parámetro dispositivo_id debe ser un entero positivo"`
- `400`: `"El parámetro limite debe ser un entero positivo"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `405`: `"Método HTTP no permitido"` (cabecera `Allow: GET, POST, OPTIONS`)
- `500`: `"Error interno al consultar los comandos"`

---

### 5.21 `POST app/comando.php`

- **Propósito:** encolar un comando remoto en estado `pendiente` para que el ESP32 lo recoja en su siguiente consulta a `GET esp32/comando.php`.
- **Autenticación:** encabezado `X-Token`.

| Campo (JSON) | Tipo | Requerido | Reglas de validación |
| --- | --- | --- | --- |
| `dispositivo_id` (o `id`) | int | Sí | Entero positivo. El dispositivo debe existir (`404`) y tener `activo = 1` (`400` si está inactivo). |
| `accion` (o `comando`) | string | Sí | No vacío; valores permitidos: `abrir_acceso`, `activar_alarma` o `alarma`. |

**Ejemplo de request:**

```bash
curl -X POST "http://localhost:8080/api_aforo/app/comando.php" \
  -H "Content-Type: application/json" \
  -H "X-Token: <token>" \
  -d '{
    "dispositivo_id": 1,
    "accion": "abrir_acceso"
  }'
```

**Respuesta exitosa (`201 Created`):**

```json
{
  "success": true,
  "mensaje": "Comando encolado exitosamente",
  "comando": {
    "id": 5,
    "dispositivo_id": 1,
    "accion": "abrir_acceso",
    "estado": "pendiente",
    "fecha_creacion": "2026-11-24 18:00:00",
    "fecha_ejecucion": null
  }
}
```

**Respuestas de error:**

- `400`: `"Cuerpo JSON inválido"`
- `400`: `"El campo dispositivo_id es obligatorio y debe ser un entero positivo"`
- `400`: `"El campo accion es obligatorio"`
- `400`: `"Acción no permitida. Solo se permite abrir_acceso o activar_alarma"`
- `400`: `"El dispositivo está inactivo"`
- `401`: `"Token inválido o ausente"`
- `404`: `"Dispositivo no encontrado"`
- `405`: `"Método HTTP no permitido"`
- `500`: `"Error interno al encolar el comando"`

---

## 6. Reglas de negocio

1. **Alternancia entrada/salida (`POST esp32/acceso.php`):** el tipo de movimiento se calcula en el servidor consultando el último registro en `registros_acceso` para el par `(persona_id, dispositivo_id)` ordenado por `fecha DESC, id DESC`. Si el último movimiento fue `entrada`, se registra `salida`; en caso contrario (sin registros previos o último registro `salida`), se registra `entrada`. La operación corre dentro de una transacción con bloqueo `FOR UPDATE` sobre el dispositivo para evitar entradas simultáneas que excedan el cupo.
2. **Control de aforo máximo (`aforo_max`):** antes de insertar una `entrada`, se compara el total actual en la vista `personas_dentro` para ese dispositivo contra `limites.aforo_max`. Si `dentro >= aforo_max`, se niega la entrada (`HTTP 200`, `"success": false`, `"error": "aforo_lleno"`), **no** se inserta fila en `registros_acceso` y se crea una alerta `tipo = 'aforo'` (`"Aforo lleno, acceso denegado"`, `atendida = 0`) únicamente si no existe ya una alerta de tipo `aforo` sin atender para ese dispositivo. Cuando el movimiento calculado es `salida`, **siempre se permite** incluso si el aforo está lleno.
3. **Alertas de medición ambiental (`POST esp32/medicion.php`):** tras guardar la medición, se evalúan `temperatura > temp_max` (`tipo = 'temperatura'`, `"Temperatura por encima del límite"`) y `humedad > hum_max` (`tipo = 'humedad'`, `"Humedad por encima del límite"`). La comparación es estricta (una lectura igual al límite no genera alerta). Para evitar duplicados, dentro de la transacción (`FOR UPDATE`) solo se inserta una nueva alerta si no existe otra sin atender (`atendida = 0`) del mismo `dispositivo_id` y `tipo`. Una vez que la alerta se marca como atendida (`atendida = 1`), una nueva lectura que vuelva a superar el límite genera una nueva alerta. El campo `"alerta": true` se devuelve siempre que algún valor supere su límite, haya o no una alerta abierta previa.
4. **Entrega única de comandos remotos (`pendiente` → `ejecutado`):** `POST app/comando.php` crea comandos con `estado = 'pendiente'`. Cuando el ESP32 invoca `GET esp32/comando.php`, el servidor selecciona dentro de una transacción con `FOR UPDATE` el comando `pendiente` más antiguo (`ORDER BY fecha_creacion ASC, id ASC LIMIT 1`), lo actualiza a `estado = 'ejecutado'` con `fecha_ejecucion = NOW()` y lo devuelve ya marcado como `ejecutado`. Así cada comando se entrega exactamente una vez.
5. **Tarjetas pendientes para UID desconocidos:** si `POST esp32/acceso.php` recibe un `uid` que no pertenece a ninguna persona con `activo = 1`, registra o actualiza el UID en `tarjetas_pendientes` (`ON DUPLICATE KEY UPDATE dispositivo_id = ?, fecha = NOW()`) y responde `HTTP 200` con `"success": false` y `"error": "tarjeta_no_registrada"`. Al ejecutar `POST app/personas.php` con `accion = "asignar"`, el UID se vincula a una persona y se elimina de `tarjetas_pendientes` dentro de la misma transacción.
6. **Reasignación de tarjeta de una persona inactiva (`POST app/personas.php`):** en las acciones `crear`, `editar` y `asignar`, antes de asignar un `uid_rfid`, el servidor busca con `FOR UPDATE` si el UID ya pertenece a otra persona. Si la persona propietaria está activa (`activo = 1`), rechaza la petición con `409` (`"La tarjeta ya pertenece a otra persona"`). Si está inactiva (`activo = 0`), libera automáticamente la tarjeta dentro de la misma transacción asignando a la persona inactiva un marcador único `SIN-<16-hex>` (dado que `uid_rfid` es `NOT NULL` y `UNIQUE`) y completa la asignación a la nueva persona.
