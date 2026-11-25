# Expediente técnico — Aforo IoT

Índice general de la documentación técnica del proyecto integrador **Aforo IoT** (control de acceso RFID y monitoreo de temperatura y humedad con ESP32, PHP 8 + MySQL y Android Java Material Design 3).

## Índice de secciones

| # | Sección del expediente | Responsable | Sprint | Documento / Ubicación | Estado |
| --- | --- | --- | --- | --- | --- |
| 1 | Portada, resumen ejecutivo, problema, justificación, objetivos y alcance | A (OsvaldoVar) | Fase 0 | [01-problema-objetivos.md](./01-problema-objetivos.md) | Disponible |
| 2 | Usuarios, requerimientos funcionales y no funcionales, casos de uso UML | C (OscarJDA) | Sprint 1 | [02-requerimientos-casos-uso.md](./02-requerimientos-casos-uso.md) | Disponible |
| 3 | Arquitectura del sistema y diagrama de despliegue | A (OsvaldoVar) | Sprint 1 | [03-arquitectura.md](./03-arquitectura.md) | Disponible |
| 4 | Modelo de datos (diagrama entidad-relación y diccionario de datos) | A (OsvaldoVar) | Sprint 2 | [04-modelo-datos.md](./04-modelo-datos.md) | Listo |
| 4 | Diagrama de conexiones del hardware | B (salazar5678-web) | Sprint 1 | `docs/hardware/` | Pendiente |
| 5 | Modelo de datos (diagrama ER en S2) y documentación de endpoints (`docs/api.md`, cierre en S3) | A (OsvaldoVar) | Sprint 2–3 | [api.md](../api.md) | Disponible |
| 6 | Protocolo y flujo de comunicación IoT, diagrama de secuencia | B (salazar5678-web) | Sprint 3 | [05-protocolo-iot.md](./05-protocolo-iot.md) | Disponible |
| 7 | Manual de instalación, configuración y ejecución | A (OsvaldoVar) | Sprint 3 | [instalacion.md](../instalacion.md) y [README.md](../../README.md) | Disponible |
| 8 | Manual de usuario con capturas | C (OscarJDA) | Sprint 3 | [manual-usuario.md](../manual-usuario.md) | Disponible |
| 9 | Reporte de pruebas: API (A), hardware (B), app (C) | Todos | Sprint 3 | [informe-pruebas-api.md](../pruebas/informe-pruebas-api.md) (API, A), [informe-pruebas-app.md](../pruebas/informe-pruebas-app.md) (app, C); [pruebas-hardware.md](../pruebas/pruebas-hardware.md) (hardware, B) | Disponible |
| 10 | Conclusiones, limitaciones, trabajo futuro y referencias | Todos | Sprint 3 | [10-conclusiones.md](./10-conclusiones.md) | Disponible |

## Convenciones de documentación

- Los documentos del expediente se mantienen en Markdown dentro de `docs/expediente/` y sus recursos auxiliares en `docs/hardware/`, `docs/postman/`, `docs/pruebas/` y `docs/capturas/`.
- Los diagramas se elaboran en Mermaid (`flowchart`) para renderizado directo en GitHub o se exportan como imágenes cuando corresponde a esquemas electrónicos o capturas de pantalla.
