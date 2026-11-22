# Mockups — Fase 0 (F0-C1)

Prototipos de las 6 pantallas principales de la app Android, con la paleta y la
tipografía de [ESTETICA.md](../ESTETICA.md). Medida base: **412 × 915 px**
(Android, densidad xxhdpi).

**Enlace de Figma:** _pendiente — pegar aquí el enlace con permiso "anyone with the link can view" antes del PR._

## Pantallas

| Archivo | Pantalla | Contenido clave |
| --- | --- | --- |
| `login.svg` | Login | Marca, campos de correo y contraseña, botón principal, enlace a registro |
| `dashboard.svg` | Dashboard | Chip de estado del ESP32, aforo con barra de progreso, clima, control y últimos accesos |
| `historial.svg` | Historial | Pestañas Accesos/Mediciones, filtro de fechas, lista de entradas y salidas |
| `personas.svg` | Personas | Personas dentro, tarjetas pendientes de asignar, FAB para registrar |
| `dispositivos.svg` | Dispositivos | Dispositivo en línea y sin conexión, aforo máximo, última conexión, FAB de alta |
| `alertas.svg` | Alertas | Filtros, alertas activas con acción Atender y alertas atendidas |

Los `*.png` son una exportación de respaldo de cada SVG. Si editas en Figma,
reemplázalos por la exportación de Figma (PNG 1x).

## Cómo importar en Figma

1. Abre un archivo nuevo en Figma.
2. Arrastra los 6 `.svg` al lienzo (o `File > Place image`).
3. Cada SVG entra como frame de 412 × 915; renómbralos `Login`, `Dashboard`, etc.
4. Opcional: duplica el Material 3 Design Kit de la Community y usa sus
   componentes (`MaterialToolbar`, `BottomNavigationView`, `Card`, `Button`).

## Cómo exportar los PNG

Selecciona cada frame → panel **Export** → `PNG`, escala `1x` → **Export**.
Guarda el archivo como `login.png`, `dashboard.png`, etc. en esta carpeta.

Sin Figma, los PNG se regeneran desde los SVG con Chrome:

```powershell
$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
foreach ($n in @("login","dashboard","historial","personas","dispositivos","alertas")) {
    & $chrome --headless --disable-gpu --hide-scrollbars --window-size=412,915 `
        --screenshot="docs/mockups/$n.png" "file:///$PWD/docs/mockups/$n.svg"
}
```

## Especificaciones usadas

- Color semilla `#00639B`; roles Material 3 de la sección 1 de ESTETICA.
- Márgenes de 16 px, separación entre tarjetas de 12 px, padding interno de 16 px.
- Tarjetas con radio de 12 px, botones tipo píldora, chips de 8 px.
- Escala tipográfica: `displaySmall` para el aforo, `headlineSmall` para clima,
  `titleMedium` para títulos, `bodyMedium` para filas, `labelLarge` para botones.
- Navegación inferior de 4 destinos: Dashboard, Historial, Personas, Más.

## Fuera de alcance

- Versiones en modo oscuro: ESTETICA define los roles para la implementación,
  pero la Fase 0 solo pide las 6 pantallas en claro.
- Pantallas de Límites y Gráficas: no están en el alcance de F0-C1.
