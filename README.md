# Aforo IoT

Sistema IoT de control de acceso con RFID y monitoreo de temperatura y humedad. Un ESP32 con lector RC522, sensor DHT11 (compatible con DHT22), LED verde, LED rojo y buzzer envía datos por HTTP (JSON) a una API REST en PHP con MySQL. Una app Android (Java, Volley, Material Design 3) muestra cuántas personas hay dentro, la temperatura y la humedad, el historial y las alertas, y permite acciones de control. Regla: la primera lectura de una tarjeta es entrada y la siguiente es salida; si se alcanza el aforo máximo, la entrada se niega (LED rojo).

## Componentes

| Carpeta | Contenido |
| --- | --- |
| `android/` | App Android |
| `backend/api_aforo/` | API REST en PHP |
| `database/` | Esquema y datos de MySQL |
| `firmware/esp32_aforo/` | Firmware del ESP32 |
| `docs/` | Documentación |

## Requisitos

- Linux con PHP 8 (extensión `pdo_mysql`) y MariaDB (o MySQL 8)
- Android Studio
- Arduino IDE con soporte para ESP32 y las librerías MFRC522, DHT sensor library y ArduinoJson 7
- Postman

## Instalación

### Base de datos y backend

En Arch Linux / CachyOS (asegurando que el firewall permita tráfico entrante en el puerto TCP 8080 dentro de la red WiFi de 2.4 GHz):

```bash
sudo pacman -S --needed php mariadb
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
sudo systemctl enable --now mariadb
sudo sed -i 's/^;extension=pdo_mysql/extension=pdo_mysql/' /etc/php/php.ini
sudo mariadb < database/schema.sql
sudo mariadb < database/seed.sql
sudo mariadb -e "CREATE USER IF NOT EXISTS 'aforo'@'localhost' IDENTIFIED BY 'cambia-esta-clave'; GRANT ALL PRIVILEGES ON aforo_iot.* TO 'aforo'@'localhost';"
cp backend/api_aforo/config.example.php backend/api_aforo/config.php   # editar DB_USER y DB_PASS
php -S 0.0.0.0:8080 -t backend
```

En otras distribuciones usar su gestor de paquetes (`apt install php php-mysql mariadb-server`); si un integrante usa Windows puede usar XAMPP, solo cambia la URL base.

URLs base de la API:

- Local: `http://localhost:8080/api_aforo/`
- Emulador Android: `http://10.0.2.2:8080/api_aforo/`
- Celular y ESP32: `http://<IP-de-la-laptop>:8080/api_aforo/`

### Firmware

Configuración de red WiFi (2.4 GHz) y URL del servidor en `firmware/esp32_aforo/config.h`. Compilación y carga en placa ESP32 Dev Module mediante Arduino IDE 2 o `arduino-cli`. Guía paso a paso, librerías y tabla de conexiones en [docs/instalacion.md](docs/instalacion.md#3-firmware-esp32).

### App

Configuración de la URL de la API en `android/local.properties`, compilación con Gradle (`./gradlew assembleDebug`) e instalación en dispositivo Android en la misma red WiFi. La URL también se puede cambiar desde la app (Login → **Configurar servidor**, o Más → **Servidor**) sin recompilar. Guía detallada en [docs/instalacion.md](docs/instalacion.md#4-aplicación-android).

## Flujo de trabajo

- Ramas: `main` = entregas, `develop` = integración
- Cada tarea en una rama `feature/<ID>-descripcion` creada desde `develop`
- Commits pequeños con prefijo `feat`, `fix`, `docs`, `test` o `refactor`
- PR hacia `develop` con `Closes #N` en la descripción
- Aprobación del administrador antes de hacer merge
- Nunca subir `config.php`, `config.h` ni contraseñas

## Equipo

| Usuario | Rol |
| --- | --- |
| OsvaldoVar | Administrador del repo, base de datos y API |
| salazar5678-web | Firmware ESP32 y hardware |
| OscarJDA | App Android |

## Documentación

- [docs/instalacion.md](docs/instalacion.md) — Manual de instalación y puesta en marcha paso a paso
- [CONTRIBUTING.md](CONTRIBUTING.md) — Flujo de ramas, commits y PR
- [docs/ESTANDARES.md](docs/ESTANDARES.md) — Reglas de código por área
- [docs/ESTETICA.md](docs/ESTETICA.md) — Guía visual Material 3 y señalización de hardware
