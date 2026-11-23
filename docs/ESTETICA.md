# Guía visual y estética Material 3 — Aforo IoT

Referencia visual para la app Android (`com.example.aforoiot`, Material Components `1.12.0`, tema `Theme.Material3.DayNight.NoActionBar`), los indicadores físicos del ESP32 y la documentación del expediente.

## 1. Paleta de colores

Color semilla del proyecto: **`#00639B`** (azul técnico IoT, definido aquí al no especificarse un valor hexadecimal en el plan). Todos los roles se declaran en `res/values/colors.xml` (claro) y `res/values-night/colors.xml` (oscuro) y se consumen mediante atributos `?attr/...`.

### Roles principales Material 3

| Rol | Claro (`values/`) | Oscuro (`values-night/`) | Uso principal |
| --- | --- | --- | --- |
| `primary` | `#00639B` | `#96CCFF` | Botón principal, barra de aforo normal, ícono activo |
| `onPrimary` | `#FFFFFF` | `#003353` | Texto e íconos sobre `primary` |
| `primaryContainer` | `#CEE5FF` | `#004A76` | Fondo de `FAB`, ítem activo en `BottomNavigationView` |
| `onPrimaryContainer` | `#001D33` | `#CEE5FF` | Contenido sobre `primaryContainer` |
| `secondary` | `#51606F` | `#B9C8DA` | Acciones secundarias, filtros, pestañas inactivas |
| `onSecondary` | `#FFFFFF` | `#233240` | Contenido sobre `secondary` |
| `secondaryContainer` | `#D5E4F7` | `#3A4857` | Superficies de apoyo, chips informativos |
| `onSecondaryContainer` | `#0E1D2A` | `#D5E4F7` | Contenido sobre `secondaryContainer` |
| `tertiary` | `#68587A` | `#D3BFE6` | Series secundarias en gráficas (humedad), distintivos |
| `onTertiary` | `#FFFFFF` | `#382A49` | Contenido sobre `tertiary` |
| `tertiaryContainer` | `#EEDBFF` | `#504061` | Contenedor de acento secundario |
| `onTertiaryContainer` | `#231533` | `#EEDBFF` | Contenido sobre `tertiaryContainer` |
| `error` | `#BA1A1A` | `#FFB4AB` | Aforo lleno, alertas críticas, botón `Activar alarma` |
| `onError` | `#FFFFFF` | `#690005` | Contenido sobre `error` |
| `errorContainer` | `#FFDAD6` | `#93000A` | Chip `ESP32 sin conexión`, tarjetas de alerta activa |
| `onErrorContainer` | `#410002` | `#FFDAD6` | Texto e ícono sobre `errorContainer` |
| `surface` | `#F7F9FF` | `#101418` | Fondo principal de pantalla |
| `onSurface` | `#181C20` | `#E0E2E8` | Texto principal y cifras |
| `surfaceContainer` | `#ECEEF4` | `#1C2024` | Fondo de `MaterialCardView` estilo `Filled` |
| `onSurfaceVariant` | `#42474E` | `#C2C7CF` | Etiquetas secundarias, fechas, íconos en reposo |
| `outline` | `#72777F` | `#8C9199` | Bordes de `TextInputLayout` y divisores |

### Roles semánticos de estado

| Estado | Contenedor (claro / oscuro) | Contenido (claro / oscuro) | Uso |
| --- | --- | --- | --- |
| En línea (`statusOnline`) | `#D7F5DF` / `#144A26` | `#0B4F24` / `#B7F0C5` | `chipEstado` cuando `en_linea == true` (`ultima_conexion < 30 s`) |
| Advertencia (`statusWarning`) | `#FFE8C2` / `#5A3B00` | `#4A2E00` / `#FFDDA5` | Barra de aforo entre 80 % y 99 % |
| Sin conexión / Alerta | `errorContainer` (`#FFDAD6` / `#93000A`) | `onErrorContainer` (`#410002` / `#FFDAD6`) | `chipEstado` sin conexión, aforo al 100 %, alertas |

## 2. Tipografía (escala Material 3)

Usar exclusivamente estilos `TextAppearance.Material3.*` mediante atributos del tema y tamaños en `sp`:

| Nivel M3 | Atributo XML | Tamaño | Uso en Aforo IoT |
| --- | --- | --- | --- |
| `displaySmall` | `?attr/textAppearanceDisplaySmall` | `36sp` | Cifra principal de aforo en `DashboardActivity` (`txtDentro`) |
| `headlineSmall` | `?attr/textAppearanceHeadlineSmall` | `24sp` | Lecturas de clima (`txtTemperatura`, `txtHumedad`) |
| `titleMedium` | `?attr/textAppearanceTitleMedium` | `16sp` | Títulos de tarjetas (`Personas dentro`, `Clima`), subtítulos de pantalla |
| `bodyMedium` | `?attr/textAppearanceBodyMedium` | `14sp` | Filas de `RecyclerView` (`rvDentro`, historial, alertas), mensajes y estados vacíos |
| `labelLarge` | `?attr/textAppearanceLabelLarge` | `14sp` | Botones (`MaterialButton`), `chipEstado`, pestañas (`TabLayout`) |

> Nota: unificar `txtDentro` con `?attr/textAppearanceDisplaySmall` para mantener el encabezado compacto en pantallas pequeñas.

## 3. Espaciado y esquinas

### Retícula de espaciado (múltiplos de `4dp` / `8dp`)

- **Margen de pantalla:** `16dp` (izquierda, derecha, superior e inferior).
- **Separación entre tarjetas:** `12dp` (`cardAforo`, `cardClima`, tarjetas de lista).
- **Padding interno de tarjetas:** `16dp`.
- **Separación interna entre elementos relacionados:** `8dp` (y `4dp` entre etiqueta y valor inmediato).
- **Separación entre bloques o secciones:** `24dp`.

### Radios de esquina (Shape M3)

| Componente | Radio | Referencia M3 |
| --- | --- | --- |
| `MaterialCardView` | `12dp` | `shapeAppearanceCornerMedium` |
| `MaterialButton` | `full` (píldora, 50 %) | `shapeAppearanceCornerFull` |
| `Chip` | `8dp` | `shapeAppearanceCornerSmall` |
| `TextInputLayout` (outlined) | `4dp`–`8dp` | Predeterminado M3 |
| `MaterialAlertDialog` | `28dp` | Predeterminado M3 |

## 4. Componentes por uso

- **`MaterialCardView`:**
  - `Widget.Material3.CardView.Filled` para contenedores de métricas en `DashboardActivity` (`cardAforo`, `cardClima`) y resúmenes de historial.
  - `Widget.Material3.CardView.Elevated` para tarjetas con interacción directa o destaque (p. ej. tarjetas pendientes por asignar en `PersonasActivity` o alertas sin atender en `AlertasActivity`).
- **`Chip` (`chipEstado`):** `Widget.Material3.Chip.Assist` no editable. Muestra `ESP32 en línea` con colores `statusOnline` cuando `en_linea` es `true`, y `ESP32 sin conexión` con `errorContainer` / `onErrorContainer` cuando es `false`.
- **`LinearProgressIndicator` (`progresoAforo`):** compara `dentro` contra `aforo_max`. Color del indicador: `primary` (< 80 %), `statusWarning` (80–99 %) y `error` (100 % o `dentro >= aforo_max`).
- **`MaterialToolbar`:** barra superior en todas las pantallas principales con título claro y menú de acciones (cerrar sesión, exportar CSV en `HistorialActivity`).
- **`BottomNavigationView`:** navegación principal de 4 destinos definidos en el plan: **Dashboard**, **Historial**, **Personas** y **Más** (acceso a Dispositivos, Alertas, Límites y Gráficas).
- **`FloatingActionButton`:** acción única de creación en pantallas de catálogo (`PersonasActivity` para registrar persona, `DispositivosActivity` para dar de alta un ESP32).
- **`Snackbar`:** reemplaza a `Toast` en toda la app para errores de red, confirmación de comandos (`abrir_acceso`, `alarma`) y guardado de límites o registros.
- **`MaterialAlertDialog` (`MaterialAlertDialogBuilder`):** confirmación obligatoria antes de acciones sensibles (`Abrir acceso`, `Activar alarma`, desactivar dispositivo, cerrar sesión).
- **Formularios y filtros:** `TextInputLayout` outlined + `MaterialButton` en `LoginActivity`, `RegistroActivity` y `LimitesActivity`; `TabLayout` (Accesos / Mediciones) + `MaterialDatePicker` de rango en `HistorialActivity`.

## 5. Iconografía

- Utilizar **Material Symbols Outlined** a **`24dp`** como vectores XML (`res/drawable/ic_<nombre>.xml`).
- Tintar con atributos del tema (`?attr/colorOnSurfaceVariant` en reposo, `?attr/colorPrimary` o el rol `on*Container` correspondiente cuando esté activo).
- Mapeo sugerido:
  - Dashboard / Aforo: `groups`, `thermostat`, `water_drop`
  - Historial y CSV: `history`, `calendar_month`, `download`
  - Personas y RFID: `badge`, `person_add`, `nfc`
  - Dispositivos y control: `memory`, `lock_open`, `notifications_active`, `tune`, `show_chart`

## 6. Estados de pantalla

- **Carga:** en carga inicial mostrar `CircularProgressIndicator` centrado; en refrescos periódicos (cada 5 s en `DashboardActivity`) actualizar sin bloquear la vista. Deshabilitar botones durante envíos `POST` para evitar peticiones duplicadas.
- **Vacío:** mostrar ícono `48dp` en `?attr/colorOnSurfaceVariant`, texto breve en `bodyMedium` (ej. *"No hay personas dentro"*, *"Sin registros en este rango"*) y un botón de acción cuando corresponda.
- **Error:** ninguna pantalla debe cerrarse por excepción de red o JSON. En fallos de conexión o servidor caído mostrar `Snackbar` con acción **Reintentar** (y vista con botón de reintento si la pantalla quedó vacía). Si el backend devuelve HTTP `401`, limpiar el token en `SessionManager` y redirigir a `LoginActivity`.

## 7. Accesibilidad

- **Contraste:** mínimo **4.5:1** para texto normal y **3:1** para texto grande (>= `18sp` negrita o `24sp`) e íconos de estado. Todos los pares de color de la sección 1 superan `6.4:1` en modo claro y oscuro.
- **Área táctil:** mínimo **`48dp × 48dp`** en botones, ítems clicables, íconos de acción y chips interactivos.
- **Descripciones:** agregar `android:contentDescription` desde `strings.xml` en todo `ImageView`, `ImageButton`, `FloatingActionButton` e ícono informativo. Marcar íconos decorativos con `android:importantForAccessibility="no"`.
- **No depender solo del color:** acompañar siempre el color con texto o ícono (`ESP32 en línea` / `ESP32 sin conexión`, porcentaje o conteo `2 / 2` junto a `progresoAforo`).

## 8. Estética del hardware (ESP32)

Pines del plan: `LED_VERDE` (GPIO 25), `LED_ROJO` (GPIO 26) y `BUZZER` (GPIO 27), cada LED con resistencia de `220 Ω`. Toda señalización corre con `millis()` sin bloquear el lector RC522.

| Evento | LED verde | LED rojo | Patrón de buzzer | Origen |
| --- | --- | --- | --- | --- |
| Acceso permitido (entrada o salida) | Encendido `1500 ms` | Apagado | Silencio | Plan |
| Tarjeta desconocida o error HTTP | Apagado | Encendido `1500 ms` | 3 pitidos de `100 ms` con pausas de `100 ms` (`600 ms` total) | Plan |
| Comando remoto `abrir_acceso` | Encendido `3000 ms` (o relevador) | Apagado | 1 pitido corto de `120 ms` al activar | LED: Plan / Buzzer: **Propuesta** |
| Aforo lleno (`aforo_lleno`) o alerta ambiental activa | Apagado | Encendido `1500 ms` | 2 pitidos largos de `300 ms` con pausa de `150 ms` | LED: Plan / Buzzer: **Propuesta** |
| Comando remoto `alarma` | Apagado | Parpadeo `200 ms` ON / `200 ms` OFF (`2000 ms`) | 5 pitidos de `200 ms` con pausas de `200 ms` (`2000 ms` total) | Activación: Plan / Patrón: **Propuesta** |

## 9. Documentación visual y diagramas

- **Diagramas en Mermaid:** arquitectura, despliegue, diagrama ER y diagrama de secuencia se escriben en bloques ` ```mermaid ` dentro de los archivos Markdown de `docs/` para versionarlos en Git y revisarlos en PR.
- **Capturas de pantalla:** guardar en formato **PNG** dentro de `docs/capturas/` con nombres en `snake_case.png` (ej. `login_claro.png`, `dashboard_oscuro.png`, `historial_accesos.png`, `alertas_aforo.png`). Los mockups de Figma de Fase 0 se guardan en `docs/mockups/`.
