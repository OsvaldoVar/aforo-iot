# Cómo trabajar en el repo

## Ramas

- `main`: entregas. Solo recibe merges de `develop` al cerrar cada sprint.
- `develop`: integración. Rama por defecto.
- `feature/<ID>-<tema>`: una rama por issue, siempre desde `develop`. Ejemplo: `feature/S1-C3-login`.

Las dos ramas están protegidas: nadie hace push directo, todo entra por PR con 1 aprobación.

## Por cada tarea

1. Toma tu issue del milestone del sprint (ya está asignado a ti).
2. Crea la rama desde `develop` actualizado.
3. Haz commits pequeños y frecuentes, varios por día. No subas una carpeta completa en un solo commit.
4. Abre un PR hacia `develop` con `Closes #N` y una captura, log o prueba de Postman.
5. Atiende los comentarios de la revisión. El merge lo hace el admin.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/S1-B2-dht22

# ...trabajo...
git add firmware/
git commit -m "feat(firmware): sketch de prueba del DHT22"
git push -u origin feature/S1-B2-dht22
# En GitHub: Compare & pull request, base develop
```

## Mensajes de commit

Prefijo y área entre paréntesis:

- `feat(backend): login con token`
- `fix(android): token vacío al reabrir la app`
- `docs(firmware): tabla de pines`
- `test(backend): colección Postman del sprint 1`

Prefijos: `feat`, `fix`, `docs`, `test`, `refactor`. Áreas: `backend`, `firmware`, `android`, `database`, `docs`.

Cada quien hace commits con su propia cuenta de GitHub: se evalúan las contribuciones por integrante.

## Secretos

Nunca subas `backend/api_aforo/config.php`, `firmware/**/config.h` ni `.env`. Ya están en `.gitignore`. Sube solo los `*.example` con valores de ejemplo.

## Sprints

| Milestone | Fechas | Tag |
| --- | --- | --- |
| Sprint 1 · Cada pieza funciona sola | sáb 21 – dom 22 nov | `v0.1` |
| Sprint 2 · Integración punta a punta | lun 23 – mar 24 nov | `v0.2` |
| Sprint 3 · Control, alertas y entrega | mié 25 – jue 26 nov | `v1.0` |

Daily por WhatsApp a las 9:00 y a las 21:00: qué hice, qué haré, qué me bloquea.

## Reglas de código

Sigue las convenciones por capa en [docs/ESTANDARES.md](docs/ESTANDARES.md) y la guía visual Material 3 y de hardware en [docs/ESTETICA.md](docs/ESTETICA.md). El formato base está definido en [.editorconfig](.editorconfig).
