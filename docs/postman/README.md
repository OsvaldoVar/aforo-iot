# Colección Postman — Aforo IoT

Pruebas automatizadas de autenticación, ESP32, consultas y gestión de la app, límites y alertas S3-A1/A2.

## Requisitos previos

Se requiere una base de datos configurada con el esquema del proyecto y `backend/api_aforo/config.php`. No reimportar `database/schema.sql` sobre una base compartida: elimina las tablas existentes. El servidor se levanta con `php -S 127.0.0.1:8081 -t backend` desde la raíz del repo.

Los datos seed incluyen el usuario `admin@aforoiot.local` (`admin123`), el dispositivo con `api_key` `demo-esp32-key-0001` y la persona Ana (`A1B2C3D4`, inicialmente fuera).

## Cómo importar en Postman

1. Abre Postman y haz clic en **Import**.
2. Selecciona el archivo `docs/postman/aforo-iot.postman_collection.json`.
3. Verifica que aparezca la colección **Aforo IoT - Sprint 1** con las carpetas `API`, `Auth` y `ESP32`.

## Configurar `base_url`

Abre la pestaña **Variables** de la colección y ajusta `base_url` según tu entorno:

- **Misma PC:** `http://localhost:8080/api_aforo`
- **Emulador:** `http://10.0.2.2:8080/api_aforo`
- **Celular:** `http://<IP-de-la-laptop>:8080/api_aforo` (por ejemplo, `http://192.168.1.50:8080/api_aforo`)

La variable `api_key` viene precargada con `demo-esp32-key-0001` y `token` se llena automáticamente al ejecutar **Login OK**.

## Ejecutar con Collection Runner

1. Haz clic en los tres puntos de la colección **Aforo IoT - Sprint 1** y elige **Run collection**.
2. Mantén el orden original de las peticiones y pulsa **Run Aforo IoT - Sprint 1**.

> **Nota:** Las peticiones **Acceso Ana 1a vez (Entrada)** y **Acceso Ana 2a vez (Salida)** deben ejecutarse en ese orden para validar correctamente la alternancia entrada/salida (con Ana partiendo fuera tras cargar `seed.sql`).

## Límites y alertas S3-A1/A2

La carpeta **S3 Límites y alertas** contiene 38 peticiones. Puede ejecutarse por separado, en orden, con un `token` válido de un usuario de pruebas y `base_url` configurada como `http://127.0.0.1:8081/api_aforo`.

```bash
newman run docs/postman/aforo-iot.postman_collection.json \
  --folder 'S3 Límites y alertas' \
  --env-var 'base_url=http://127.0.0.1:8081/api_aforo' \
  --env-var "token=$TOKEN_PRUEBAS" \
  --export-environment /tmp/aforo-s3-entorno.json \
  --export-collection /tmp/aforo-s3-resultado.json
```

La carpeta crea un dispositivo y dos personas con tarjetas aleatorias. Conserva sus identificadores en las variables `s3_dispositivo_id`, `s3_persona_1_id` y `s3_persona_2_id` de la colección; Newman los exporta a `/tmp/aforo-s3-resultado.json`. No cambia los límites de otros dispositivos. La colección completa incluye otras pruebas que usan datos seed y crean recursos propios; revisar esos efectos antes de ejecutarla en una base compartida.

- `GET app/limites.php?dispositivo_id=...` devuelve `limites` con `dispositivo_id`, `temp_max`, `hum_max` y `aforo_max` numéricos.
- `POST` y `PUT app/limites.php` guardan esos cuatro campos. Temperatura entre 0 y 60 °C, humedad entre 0 y 100 %, aforo entero positivo hasta el máximo de la columna (`4294967295`). Los valores inválidos devuelven HTTP 400; los dispositivos inexistentes, 404.
- `GET app/alertas.php` devuelve `alertas` y `total`, ordenadas por fecha e ID descendentes. Filtros opcionales: `atendida=0|1`, `dispositivo_id` y `limite` (también `limit`), por defecto 100 y máximo 1000.
- `POST app/alertas.php` con `{"id": ...}` marca la alerta como atendida. Repetir la petición es válido; un ID inexistente devuelve 404.
- Todos los endpoints `app/*` requieren `X-Token`. Las respuestas mantienen `success`, `mensaje` y campos adicionales planos.
- Una medición estrictamente superior al límite genera una alerta por tipo y dispositivo, sin duplicar alertas abiertas. `alerta` es un booleano que indica si la medición actual supera algún límite, incluso si la alerta ya existía. Atender una alerta permite generar otra si vuelve a superarse el límite; una medición normal no atiende las anteriores.
- Una entrada con aforo lleno devuelve HTTP 200, `success: false`, `error: "aforo_lleno"`, `nombre` y `dentro`; no registra el acceso. Las salidas siguen permitidas. Los dispositivos sin una fila de límites conservan el comportamiento previo.

Las pruebas cubren POST/PUT/GET, rangos y tipos inválidos, autenticación, igualdad exacta con el límite, temperatura y humedad por separado, duplicados, atención y reapertura, filtros y aforo. La última entrada de la persona rechazada demuestra que el rechazo anterior no dejó un registro; ambas personas terminan fuera.

Al terminar, eliminar únicamente los recursos creados por esa ejecución. Usar los tres identificadores exportados y verificar que correspondan a los recursos `Prueba S3` antes de limpiar. Dentro de una transacción, eliminar primero `registros_acceso` del dispositivo de prueba, después el dispositivo (sus límites, mediciones y alertas se eliminan por cascada), y finalmente las dos personas. No borrar recursos ajenos ni reiniciar la base de datos.
