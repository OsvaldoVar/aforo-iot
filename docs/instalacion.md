# Manual de instalación y puesta en marcha — Aforo IoT

Este manual describe el procedimiento para instalar, configurar, compilar y verificar todos los componentes del sistema Aforo IoT desde cero.

---

## 1. Requisitos previos

Asegúrate de contar con las siguientes herramientas en tu entorno:

- **PHP 8** con la extensión `pdo_mysql` habilitada.
- **MariaDB 10.4+** o **MySQL 8.0+**.
- **JDK 17 a 21** para compilar el proyecto Android.
  > **Importante:** Gradle 8.11.1 no es compatible con Java 25. Si tu sistema tiene una versión superior, debes definir `JAVA_HOME` apuntando a un JDK entre 17 y 21 (recomendado JDK 21).
- **Android Studio** (con SDK de Android instalado, compileSdk 35, minSdk 24).
- **Arduino IDE 2** o **arduino-cli** con soporte para la plataforma ESP32 (`esp32:esp32`) y las siguientes librerías instaladas:
  - `MFRC522`
  - `DHT sensor library`
  - `Adafruit Unified Sensor`
  - `ArduinoJson` (versión 7)
- **Postman** o **newman** (para pruebas automatizadas de la API).

---

## 2. Base de datos y API (Backend)

### Instalación de paquetes y servicio de base de datos

Elige la instrucción según tu sistema operativo:

- **Arch Linux / CachyOS:**
  ```bash
  sudo pacman -S --needed php mariadb
  sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
  sudo systemctl enable --now mariadb
  sudo sed -i 's/^;extension=pdo_mysql/extension=pdo_mysql/' /etc/php/php.ini
  ```

- **Debian / Ubuntu:**
  ```bash
  sudo apt update
  sudo apt install -y php php-mysql mariadb-server
  sudo systemctl enable --now mariadb
  ```

- **Windows (XAMPP):**
  1. Instala e inicia XAMPP.
  2. Arranca los módulos **Apache** y **MySQL** desde el panel de control.
  3. Verifica en `php.ini` que la línea `extension=pdo_mysql` no esté comentada con punto y coma.

### Inicialización y reinicio de la base de datos

El archivo `database/schema.sql` contiene sentencias `DROP TABLE / VIEW IF EXISTS`, por lo que reimportarlo junto con `database/seed.sql` reinicia la base de datos a su estado limpio inicial.

Ejecuta desde la raíz del proyecto:

```bash
sudo mariadb < database/schema.sql
sudo mariadb < database/seed.sql
```

*(En sistemas con MySQL o XAMPP, ejecuta `mysql -u root -p < database/schema.sql` y `mysql -u root -p < database/seed.sql`).*

### Crear usuario de base de datos

Crea el usuario `aforo` y concédele permisos sobre la base de datos `aforo_iot`:

```bash
sudo mariadb -e "CREATE USER IF NOT EXISTS 'aforo'@'localhost' IDENTIFIED BY 'cambia-esta-clave'; GRANT ALL PRIVILEGES ON aforo_iot.* TO 'aforo'@'localhost'; FLUSH PRIVILEGES;"
```

### Configuración del backend

Copia la plantilla de configuración y ajusta las credenciales locales:

```bash
cp backend/api_aforo/config.example.php backend/api_aforo/config.php
```

Edita `backend/api_aforo/config.php` si cambiaste la contraseña, el puerto o el usuario:

```php
define('DB_HOST', 'localhost');
define('DB_PORT', 3306);
define('DB_NAME', 'aforo_iot');
define('DB_USER', 'aforo');
define('DB_PASS', 'cambia-esta-clave');
```

### Arrancar el servidor local

Desde la raíz del repositorio, inicia el servidor embebido de PHP en el puerto 8080:

```bash
php -S 0.0.0.0:8080 -t backend
```

> **Aviso de red:** Si la máquina tiene firewall activo, asegúrate de permitir tráfico entrante en el puerto TCP 8080 para que el ESP32 y el celular puedan comunicarse con la API.

### Verificación de la API con curl

1. **Estado general de la API:**
   ```bash
   curl -i http://localhost:8080/api_aforo/
   ```
   Respuesta esperada: código HTTP 200 y JSON con `"success": true`:
   ```json
   {"success":true,"mensaje":"API Aforo IoT operativa","servicio":"api_aforo","version":"0.1.0","configurado":true,"fecha_servidor":"..."}
   ```

2. **Inicio de sesión con usuario semilla:**
   `database/seed.sql` incluye el usuario administrador de prueba `admin@aforoiot.local` con contraseña `admin123`:
   ```bash
   curl -i -X POST http://localhost:8080/api_aforo/auth/login.php \
     -H "Content-Type: application/json" \
     -d '{"correo":"admin@aforoiot.local","password":"admin123"}'
   ```
   Respuesta esperada: código HTTP 200 con `"success": true`, `token`, `nombre`, `rol` y `usuario_id`.

---

## 3. Firmware (ESP32)

### Conexiones de hardware

Conecta los componentes al ESP32 con la placa desconectada de la alimentación:

| Componente | Pines del ESP32 | Conexión y notas |
| --- | --- | --- |
| **Alimentación protoboard** | 3.3 V, GND | Riel de 3.3 V y riel de tierra común (GND). |
| **Lector RC522 (RFID)** | GPIO 5, 18, 19, 22, 23 | GPIO 5 → SDA/SS, GPIO 18 → SCK, GPIO 19 → MISO, GPIO 22 → RST, GPIO 23 → MOSI. Alimentar a 3.3 V y GND. |
| **Sensor DHT11** (o DHT22) | GPIO 4 | El kit trae DHT11; para un DHT22 agrega `#define DHT_TIPO DHT22` en `config.h`. Pin DATA a GPIO 4. Alimentar a 3.3 V y GND. Conectar resistencia pull-up de 10 kΩ entre DATA y 3.3 V si el módulo no la incluye integrada. |
| **LED verde (Acceso)** | GPIO 25 | Ánodo a GPIO 25 mediante resistencia de 220 Ω; cátodo a GND. |
| **LED rojo (Denegado/Lleno)** | GPIO 26 | Ánodo a GPIO 26 mediante resistencia de 220 Ω; cátodo a GND. |
| **Buzzer** | GPIO 27 | Terminal positivo a GPIO 27; terminal negativo a GND. |

### Configuración del firmware

1. Copia la plantilla de configuración en la carpeta del firmware:
   ```bash
   cp firmware/esp32_aforo/config.example.h firmware/esp32_aforo/config.h
   ```
2. Abre `firmware/esp32_aforo/config.h` y edita los valores:
   ```c
   #define WIFI_SSID "NombreDeTuRed"          // Red WiFi en banda 2.4 GHz
   #define WIFI_PASS "ContraseñaDeTuRed"
   #define SERVER "http://<IP-de-la-laptop>:8080/api_aforo/"  // Incluye http://, puerto 8080 y barra final
   #define API_KEY "demo-esp32-key-0001"     // Copia de la app en Dispositivos, o usa la del seed
   ```

### Compilación y carga

#### Opción A: Desde Arduino IDE 2
1. Abre el archivo `firmware/esp32_aforo/esp32_aforo.ino`.
2. Instala la plataforma ESP32 desde **Tools > Board > Boards Manager** buscando `esp32`.
3. Instala desde **Tools > Manage Libraries** las librerías: `MFRC522`, `DHT sensor library`, `Adafruit Unified Sensor` y `ArduinoJson` (versión 7).
4. En **Tools > Board**, selecciona **ESP32 Dev Module**.
5. Selecciona el puerto serie correspondiente y presiona **Upload**.
6. Abre el Monitor Serie configurado a **115200 baudios** para observar los intentos de conexión WiFi y envíos HTTP.

#### Opción B: Con `arduino-cli`
1. Instala el core y las dependencias:
   ```bash
   arduino-cli core install esp32:esp32
   arduino-cli lib install "MFRC522" "DHT sensor library" "Adafruit Unified Sensor" "ArduinoJson@^7"
   ```
2. Compila el sketch:
   ```bash
   arduino-cli compile --fqbn esp32:esp32:esp32 firmware/esp32_aforo
   ```
3. Carga el firmware conectando la placa (reemplaza `/dev/ttyUSB0` por tu puerto serie):
   ```bash
   arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 firmware/esp32_aforo
   ```

---

## 4. Aplicación Android

### Configuración en Android Studio

1. Abre Android Studio y selecciona **Open**, eligiendo la carpeta `android/` del repositorio.
2. Espera a que concluya la sincronización inicial de Gradle.
3. Configura el archivo `android/local.properties` (créalo en la raíz de `android/` si no existe) definiendo la ruta del SDK y la URL base del backend:
   ```properties
   sdk.dir=/ruta/a/tu/Android/Sdk
   aforo.baseUrl=http://<IP-de-la-laptop>:8080/api_aforo/
   ```
   > **Nota para emulador:** Si vas a ejecutar la app en el emulador de Android Studio, usa la IP virtual del anfitrión:
   > ```properties
   > aforo.baseUrl=http://10.0.2.2:8080/api_aforo/
   > ```

   > **Cambiar la URL sin recompilar:** desde la pantalla de login con **Configurar servidor**, o en **Más → Servidor** ya con sesión. La URL se guarda en la app y tiene prioridad sobre `local.properties`.

### Compilación

Compila la versión debug mediante Gradle desde la terminal:

```bash
cd android
./gradlew assembleDebug
```

> **Aviso de JDK:** Si se presenta un fallo de Gradle con versiones recientes de Java (como Java 25), define `JAVA_HOME` hacia un JDK 21 o 17 antes de ejecutar Gradle:
> ```bash
> export JAVA_HOME=/ruta/al/jdk-21
> ./gradlew assembleDebug
> ```

El archivo APK compilado se genera en:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Instalación y primer inicio

1. Conecta tu teléfono por USB con la depuración USB activada y asegúrate de que esté conectado a la **misma red WiFi** que la laptop donde corre el backend.
2. Instala la app con adb:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   *(También puedes transferir y abrir el APK directamente en el teléfono).*
3. Abre la aplicación **Aforo IoT** en el celular.
4. Inicia sesión con las credenciales de prueba del seed:
   - **Correo:** `admin@aforoiot.local`
   - **Contraseña:** `admin123`
5. Verifica el acceso al Dashboard, visualización de aforo y estado de conexión del ESP32.

---

## 5. Pruebas con Postman Runner y newman

### Importar la colección en Postman

1. Abre Postman y pulsa en **Import**.
2. Selecciona el archivo `docs/postman/aforo-iot.postman_collection.json`.
3. En la colección importada (**Aforo IoT - Sprint 1**), entra a la pestaña **Variables** y ajusta:
   - `base_url`: `http://localhost:8080/api_aforo` (o la IP correspondiente a tu servidor).
   - `api_key`: `demo-esp32-key-0001`.

### Ejecución en Collection Runner

1. Haz clic en los tres puntos de la colección y selecciona **Run collection**.
2. **Orden de ejecución:** Mantén el orden predeterminado.
   - La petición **Login OK** (en `Auth`) ejecuta un script que asigna la variable `token` de forma automática, necesaria para las pruebas posteriores de la carpeta `App`.
   - Las peticiones **Acceso Ana 1a vez (Entrada)** y **Acceso Ana 2a vez (Salida)** deben correr en esa secuencia exacta para comprobar la alternancia de entrada y salida con Ana iniciando fuera según `seed.sql`.
   - Las carpetas **S3 Límites y alertas** y **S3 Comandos** requieren ejecutarse en orden estricto de inicio a fin.
3. Pulsa **Run Aforo IoT - Sprint 1**.

### Ejecución desde terminal con newman

Para ejecutar la colección completa:

```bash
newman run docs/postman/aforo-iot.postman_collection.json \
  --env-var 'base_url=http://localhost:8080/api_aforo'
```

Para ejecutar únicamente una carpeta en específico (por ejemplo `ESP32`):

```bash
newman run docs/postman/aforo-iot.postman_collection.json \
  --folder 'ESP32' \
  --env-var 'base_url=http://localhost:8080/api_aforo'
```

---

## 6. Problemas comunes y soluciones

- **Firewall bloqueando el puerto 8080:**
  - *Síntoma:* El ESP32 o el celular no pueden comunicarse con el backend (errores de timeout o conexión rechazada).
  - *Solución:* Habilita el puerto TCP 8080 en el firewall del anfitrión:
    - Arch / Firewalld: `sudo firewall-cmd --add-port=8080/tcp --permanent && sudo firewall-cmd --reload`
    - Ubuntu / UFW: `sudo ufw allow 8080/tcp`
    - Windows: Crear regla de entrada en el Firewall de Windows Defender para el puerto TCP 8080.

- **Red WiFi de 5 GHz no soportada por el ESP32:**
  - *Síntoma:* El ESP32 no conecta a la red y el monitor serie repite intentos de conexión.
  - *Solución:* El ESP32 únicamente soporta frecuencias de 2.4 GHz (802.11 b/g/n). Asegúrate de configurar en `config.h` una red que opere en 2.4 GHz. Si tu router emite ambas bandas con el mismo nombre, separa los SSID o crea un punto de acceso exclusivo de 2.4 GHz.

- **URL base en emulador vs celular físico:**
  - *Síntoma:* La app no carga datos y muestra error de red.
  - *Solución:*
    - En el **emulador oficial de Android**, usa `http://10.0.2.2:8080/api_aforo/` para acceder al localhost de tu computadora.
    - En un **celular físico por WiFi**, debes usar la IP local de tu computadora en la red (`http://<IP-de-la-laptop>:8080/api_aforo/`).
    - Si el celular está conectado por **cable USB**, puedes redirigir el puerto con `adb reverse tcp:8080 tcp:8080` y usar `http://127.0.0.1:8080/api_aforo/`.

- **Error HTTP 401 por token no enviado o inválido:**
  - *Síntoma:* Los endpoints bajo `app/*` devuelven HTTP 401 con mensaje de error de autenticación.
  - *Solución:* La app y las pruebas deben incluir la cabecera `X-Token: <token>` (no usar `Authorization`). Realiza una petición exitosa a `auth/login.php` para obtener un token nuevo. Cada inicio de sesión genera un token nuevo y reemplaza al anterior.

- **Gradle falla con Java 25:**
  - *Síntoma:* Error al ejecutar `./gradlew assembleDebug`: `Unsupported class file major version` o incompatibilidad detectada en el daemon de Gradle 8.11.1.
  - *Solución:* Gradle 8.11.1 solo soporta hasta JDK 21. Configura `JAVA_HOME` apuntando a un JDK 17 o 21 en tu terminal:
    ```bash
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk  # Ajustar a la ruta en tu sistema
    ```
    En Android Studio, ve a **Settings > Build, Execution, Deployment > Build Tools > Gradle** y en **Gradle JDK** selecciona una versión entre JDK 17 y JDK 21.
