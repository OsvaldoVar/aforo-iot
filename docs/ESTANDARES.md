# Estándares de código — Aforo IoT

Reglas prácticas para mantener un código uniforme entre los tres integrantes en todas las capas del proyecto (MySQL, PHP 8, Android Java y ESP32). Todo el equipo aplica este documento y la configuración de [.editorconfig](../.editorconfig).

## 1. Reglas generales

- **Codificación y formato:** UTF-8, fin de línea `LF`, sin tabulaciones, sin espacios al final de línea y salto de línea final en cada archivo.
- **Indentación:** 4 espacios en PHP, Java, SQL y Arduino/C++; 2 espacios en XML, JSON y YAML.
- **Identificadores:** en español, sin acentos ni `ñ` (`personas`, `dispositivos`, `mediciones`, `registros_acceso`, `limites`, `en_linea`, `anio`).
- **Comentarios:** en español, breves y enfocados en el *por qué*, no en repetir lo que ya dice el código.
- **Limpieza:** no dejar código muerto, variables sin usar ni bloques comentados olvidados al cerrar un PR.
- **Secretos:** nunca subir contraseñas, tokens, `api_key` reales, IPs privadas fijas ni archivos `config.php`, `config.h` o `.env`. Versionar únicamente `config.example.php`, `config.example.h` y `.env.example`.

```text
Correcto:   int aforoMax = 20;      // Limite configurable por dispositivo
Incorrecto: int aforoMáx = 20;      // Acentos o eñes en identificadores y comentarios obvios
```

## 2. SQL y MySQL

- **Tablas:** plural en `snake_case` (`usuarios`, `personas`, `tarjetas_pendientes`, `dispositivos`, `registros_acceso`, `mediciones`, `limites`, `alertas`, `comandos`) y vista `personas_dentro`.
- **Columnas:** `snake_case` sin acentos (`uid_rfid`, `ultima_conexion`, `password_hash`, `aforo_max`, `temp_max`, `hum_max`, `atendida`).
- **Llave primaria:** `id INT AUTO_INCREMENT PRIMARY KEY` como regla general (salvo tablas 1:1 o clave natural definidas en el modelo, como `limites.dispositivo_id` o `tarjetas_pendientes.uid_rfid`).
- **Llaves foráneas:** `<tabla_singular>_id` (`persona_id`, `dispositivo_id`) con restricción `FOREIGN KEY` explícita.
- **Fechas y horas:** conservar los nombres del modelo: `fecha` (`registros_acceso`, `mediciones`, `alertas`, `tarjetas_pendientes`), `ultima_conexion` (`dispositivos`), `fecha_creacion` y `fecha_ejecucion` (`comandos`), y `hora_entrada` en la vista `personas_dentro`.
- **Motor y charset:** `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.
- **Palabras clave:** en `MAYUSCULAS` (`SELECT`, `INSERT`, `UPDATE`, `FROM`, `WHERE`, `JOIN`, `ORDER BY`, `LIMIT`).

```sql
-- Correcto
CREATE TABLE registros_acceso (
    id INT AUTO_INCREMENT PRIMARY KEY,
    persona_id INT NOT NULL,
    dispositivo_id INT NOT NULL,
    tipo ENUM('entrada', 'salida') NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (persona_id) REFERENCES personas(id),
    FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Incorrecto (singular, mayúsculas inconsistentes, sin motor/charset, FK mal nombrada)
create table RegistroAcceso (
    idAcceso int primary key,
    id_persona int,
    fechaRegistro datetime
);
```

## 3. API REST y JSON

- **Claves JSON:** siempre en `snake_case` (`api_key`, `dispositivo_id`, `aforo_max`, `en_linea`, `hora_entrada`, `uid_rfid`).
- **Estructura de respuesta:** todas las respuestas devuelven JSON con `"success"` (`bool`) y `"mensaje"` (`string`), siguiendo la forma del plan:
  - En endpoints ya definidos en el plan (`esp32/acceso.php`, `app/estado.php`), se incluyen además los campos específicos acordados (`tipo`, `nombre`, `dentro`, `error`, `aforo_max`, `en_linea`, `medicion`) para compatibilidad directa con el firmware y `DashboardActivity`.
  - En endpoints de colecciones o CRUD (`app/dentro.php`, `app/historial_accesos.php`, `app/historial_mediciones.php`, `app/personas.php`, `app/dispositivos.php`, `app/alertas.php`, `app/limites.php`), usar el sobre uniforme `{"success": bool, "mensaje": string, "data": ...}` (objeto o arreglo; `null` si no aplica).
- **Códigos HTTP:**
  - `200 OK`: operación exitosa (y rechazos de negocio hacia el ESP32 como `aforo_lleno` o tarjeta desconocida para que `esp32_aforo.ino` lea el JSON sin tratarlo como error de red).
  - `201 Created`: registro creado (`auth/registro.php`, altas de recursos).
  - `400 Bad Request`: parámetros faltantes o fuera de rango (ej. temperatura fuera de 0–60 °C, humedad fuera de 0–100 %, `aforo_max <= 0`).
  - `401 Unauthorized`: cabecera `X-Token` ausente/inválida (`auth/validar_token.php`) o `api_key` rechazada.
  - `405 Method Not Allowed`: verbo HTTP distinto al esperado.
  - `500 Internal Server Error`: fallo inesperado del servidor o base de datos.
- **Fechas:** texto en formato `YYYY-MM-DD HH:MM:SS` (ej. `"2026-11-23 18:40:05"`).
- **Autenticación:**
  - **App Android (`app/*`):** cabecera HTTP `X-Token` (no usar `Authorization`, ya que Apache en XAMPP suele omitirla).
  - **ESP32 (`esp32/*`):** `api_key` en el cuerpo JSON para `POST esp32/acceso.php` y `POST esp32/medicion.php`; para `GET esp32/comando.php`, enviar `api_key` como parámetro de consulta o cambiar el endpoint a `POST` en `docs/api.md` si se envía en el cuerpo.

```json
// Correcto (respuesta de listado con sobre uniforme)
{
  "success": true,
  "mensaje": "Personas dentro obtenidas",
  "data": [
    {
      "id": 3,
      "nombre": "Ana Lopez",
      "dispositivo_id": 1,
      "hora_entrada": "2026-11-23 10:15:00"
    }
  ]
}
```

```json
// Correcto (respuesta de esp32/acceso.php segun contrato del plan)
{
  "success": false,
  "mensaje": "Aforo lleno, acceso denegado",
  "error": "aforo_lleno",
  "nombre": "Ana Lopez",
  "dentro": 2
}
```

```json
// Incorrecto (camelCase, clave message inconsistente, fecha no estandar)
{
  "ok": 1,
  "message": "Listo",
  "horaEntrada": "23/11/2026 10:15 AM"
}
```

## 4. Backend PHP 8

- **Archivos:** `snake_case.php` (`conexion.php`, `validar_token.php`, `historial_accesos.php`, `historial_mediciones.php`).
- **Nombres:** funciones y variables en `camelCase` (`$apiKey`, `$aforoMax`, `obtenerUltimoAcceso()`), constantes en `MAYUSCULAS` (`DB_HOST`, `SEGUNDOS_EN_LINEA`).
- **Tipado estricto:** `declare(strict_types=1);` es opcional, recomendado en archivos utilitarios o nuevos.
- **Cabecera JSON:** declarar `header("Content-Type: application/json; charset=utf-8");` al inicio de todo endpoint (excepto `app/exportar.php` que envía `text/csv; charset=utf-8`).
- **Consultas SQL:** usar siempre PDO con sentencias preparadas (`prepare` + `execute`). Nunca concatenar entradas del cliente en la consulta.
- **Manejo de errores:** nunca exponer mensajes de `PDOException`, SQL ni rutas internas al cliente. Registrar el detalle con `error_log()` y devolver un mensaje genérico con código `500`.

```php
<?php
// Correcto
declare(strict_types=1);

require_once "../conexion.php";
header("Content-Type: application/json; charset=utf-8");

$datos = json_decode(file_get_contents("php://input"), true) ?? [];
$apiKey = trim((string) ($datos["api_key"] ?? ""));

try {
    $consulta = $pdo->prepare("SELECT id FROM dispositivos WHERE api_key = ? AND activo = 1");
    $consulta->execute([$apiKey]);
    $dispositivo = $consulta->fetch();
} catch (PDOException $e) {
    error_log("Error en consulta de dispositivo: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "mensaje" => "Error interno del servidor"]);
    exit;
}
```

```php
<?php
// Incorrecto (inyeccion SQL y fuga de error interno)
$api_key = $_POST["api_key"];
$res = $pdo->query("SELECT id FROM dispositivos WHERE api_key = '$api_key'")
    or die($pdo->errorInfo()[2]);
```

## 5. App Android (Java + Volley + Material 3)

- **Paquete base:** `com.example.aforoiot`.
- **Nombres en Java:** clases en `PascalCase`, métodos y variables en `camelCase`, constantes `static final` en `UPPER_SNAKE` (`BASE_URL`, `INTERVALO_REFRESCO_MS`).
- **Activities:** sufijo `Activity` (`LoginActivity`, `RegistroActivity`, `DashboardActivity`, `HistorialActivity`, `PersonasActivity`, `DispositivosActivity`, `GraficasActivity`, `LimitesActivity`, `AlertasActivity`). Clases auxiliares: `Config`, `SessionManager`.
- **Layouts XML:** `activity_<nombre>.xml` (`activity_login.xml`, `activity_dashboard.xml`) para pantallas e `item_<nombre>.xml` (`item_dentro.xml`, `item_persona.xml`, `item_alerta.xml`) para filas de `RecyclerView`.
- **IDs de vista:** `camelCase` con prefijo de tipo según el plan:
  - `txt` (`TextView`: `txtDentro`, `txtTemperatura`, `txtHumedad`)
  - `btn` (`MaterialButton`: `btnLogin`, `btnAbrirAcceso`, `btnActivarAlarma`)
  - `til` / `et` (`TextInputLayout` / `TextInputEditText`: `tilCorreo`, `etCorreo`)
  - `rv` (`RecyclerView`: `rvDentro`, `rvPersonas`, `rvAlertas`)
  - `card` (`MaterialCardView`: `cardAforo`, `cardClima`)
  - `chip` (`Chip`: `chipEstado`)
  - `progreso` (`LinearProgressIndicator`: `progresoAforo`)
  - `toolbar`, `bottomNav`, `fab` (`MaterialToolbar`, `BottomNavigationView`, `FloatingActionButton`)
- **Textos:** todos los textos visibles van en `res/values/strings.xml` (`@string/...` y `getString(R.string.xxx)`). Nunca escribir textos literales para el usuario en XML ni Java.
- **Colores:** usar siempre atributos del tema (`?attr/colorPrimary`, `?attr/colorOnSurface`, `?attr/colorError`), nunca valores hexadecimales fijos en layouts ni en código Java. Ver roles completos en [ESTETICA.md](ESTETICA.md).

```xml
<!-- Correcto (2 espacios, ID con prefijo, texto en strings.xml, color por tema) -->
<TextView
  android:id="@+id/txtTemperatura"
  android:layout_width="0dp"
  android:layout_height="wrap_content"
  android:layout_weight="1"
  android:text="@string/temperatura_vacia"
  android:textAppearance="?attr/textAppearanceHeadlineSmall"
  android:textColor="?attr/colorOnSurface" />

<!-- Incorrecto (ID generico, texto fijo, color hexadecimal) -->
<TextView
    android:id="@+id/textView1"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="-- °C"
    android:textColor="#000000" />
```

```java
// Correcto
public class DashboardActivity extends AppCompatActivity {
    private static final long INTERVALO_REFRESCO_MS = 5000L;
    private TextView txtTemperatura;
    private Chip chipEstado;

    private void actualizarEstadoConexion(boolean enLinea) {
        chipEstado.setText(enLinea ? R.string.esp32_en_linea : R.string.esp32_sin_conexion);
    }
}

// Incorrecto (constante sin UPPER_SNAKE, texto hardcodeado)
public class Dashboard extends AppCompatActivity {
    int tiempo = 5000;
    Chip c1;

    void setChip(boolean ok) {
        c1.setText(ok ? "ESP32 en línea" : "ESP32 sin conexión");
    }
}
```

## 6. Firmware Arduino / ESP32

- **Constantes y pines:** `#define` en `MAYUSCULAS` usando los pines del plan (`SS_PIN 5`, `RST_PIN 22`, `DHT_PIN 4`, `LED_VERDE 25`, `LED_ROJO 26`, `BUZZER 27`). Constantes de intervalos también en `MAYUSCULAS` (`INTERVALO_MEDICION`, `INTERVALO_COMANDO`, `ANTIRREBOTE_RFID_MS`).
- **Funciones y variables:** `camelCase` (`leerUid()`, `enviarAcceso()`, `enviarMedicion()`, `actualizarIndicador()`, `ultimaMedicion`).
- **Variables globales:** limitarlas al mínimo necesario (periféricos `rfid`, `dht` y estado de temporizadores no bloqueantes).
- **Sin bloqueos en `loop()`:** usar `millis()` para mediciones (15 s), consulta de comandos (3 s), antirrebote RFID (3 s) e indicadores LED/buzzer. No usar `delay()` dentro de `loop()` (solo se admite en `setup()` al esperar WiFi).
- **Monitor serie:** siempre `Serial.begin(115200);`.
- **Secretos:** definir `WIFI_SSID`, `WIFI_PASS`, `SERVER` y `API_KEY` en `firmware/esp32_aforo/config.h` (ignorado en `.gitignore`) y mantener actualizada la plantilla `firmware/esp32_aforo/config.example.h`.

```cpp
// Correcto
#define DHT_PIN 4
#define LED_VERDE 25
#define INTERVALO_MEDICION 15000UL

unsigned long ultimaMedicion = 0;

void setup() {
    Serial.begin(115200);
    pinMode(LED_VERDE, OUTPUT);
}

void loop() {
    actualizarIndicador();
    if (millis() - ultimaMedicion >= INTERVALO_MEDICION) {
        ultimaMedicion = millis();
        enviarMedicion();
    }
}
```

```cpp
// Incorrecto (credenciales en codigo, baudios distintos, delay bloqueante en loop)
const char* pass = "MiClaveWiFi123";
int led = 25;

void setup() {
    Serial.begin(9600);
}

void loop() {
    enviarMedicion();
    delay(15000); // Bloquea la lectura de tarjetas RC522
}
```

## 7. Commits y ramas

El flujo de ramas (`main`, `develop`, `feature/<ID>-<tema>`), el formato de mensajes de commit (`<prefijo>(<area>): <descripcion>`), la política de PR y el manejo de secretos están documentados en [CONTRIBUTING.md](../CONTRIBUTING.md). Consúltalo antes de abrir cualquier rama o Pull Request.
