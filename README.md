# Aforo IoT

Sistema IoT de control de acceso con RFID y monitoreo de temperatura y humedad. Un ESP32 con lector RC522, sensor DHT22, LED verde, LED rojo y buzzer envía datos por HTTP (JSON) a una API REST en PHP con MySQL. Una app Android (Java, Volley, Material Design 3) muestra cuántas personas hay dentro, la temperatura y la humedad, el historial y las alertas, y permite acciones de control. Regla: la primera lectura de una tarjeta es entrada y la siguiente es salida; si se alcanza el aforo máximo, la entrada se niega (LED rojo).

## Componentes

| Carpeta | Contenido |
| --- | --- |
| `android/` | App Android |
| `backend/api_aforo/` | API REST en PHP |
| `database/` | Esquema y datos de MySQL |
| `firmware/esp32_aforo/` | Firmware del ESP32 |
| `docs/` | Documentación |

## Requisitos

- XAMPP con PHP 8 y MySQL
- Android Studio
- Arduino IDE con soporte para ESP32 y las librerías MFRC522, DHT sensor library y ArduinoJson 7
- Postman

## Instalación

Se completará al final del proyecto.

- Base de datos
- Backend
- Firmware
- App

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

- [CONTRIBUTING.md](CONTRIBUTING.md) — Flujo de ramas, commits y PR
- [docs/ESTANDARES.md](docs/ESTANDARES.md) — Reglas de código por área
- [docs/ESTETICA.md](docs/ESTETICA.md) — Guía visual Material 3 y señalización de hardware
