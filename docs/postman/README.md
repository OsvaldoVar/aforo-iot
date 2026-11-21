# Colección Postman — Aforo IoT (Sprint 1)

Pruebas automatizadas para los endpoints del Sprint 1 (`index.php`, `auth/registro.php`, `auth/login.php`, `esp32/medicion.php` y `esp32/acceso.php`).

## Requisitos previos

Antes de ejecutar las peticiones, asegúrate de importar con `sudo mariadb < database/schema.sql` y luego `sudo mariadb < database/seed.sql`. El servidor se levanta con `php -S 0.0.0.0:8080 -t backend` desde la raíz del repo.

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
