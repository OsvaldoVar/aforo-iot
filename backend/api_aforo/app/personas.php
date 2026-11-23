<?php
// Gestión de personas registradas y asignación de tarjetas RFID pendientes.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

$metodo = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if ($metodo !== 'GET' && $metodo !== 'POST') {
    header('Allow: GET, POST, OPTIONS');
    responder(false, 'Método HTTP no permitido', null, 405);
}

/**
 * Normaliza una fila de personas al formato JSON de respuesta.
 *
 * @param array<string, mixed> $fila
 * @return array<string, mixed>
 */
function formatearPersona(array $fila): array
{
    return [
        'id' => (int) $fila['id'],
        'nombre' => (string) $fila['nombre'],
        'uid_rfid' => (string) $fila['uid_rfid'],
        'activo' => ((int) $fila['activo']) === 1,
    ];
}

/**
 * Obtiene una persona por su ID.
 *
 * @return array<string, mixed>|null
 */
function obtenerPersonaPorId(PDO $pdo, int $personaId): ?array
{
    $consulta = $pdo->prepare(
        'SELECT id, nombre, uid_rfid, activo FROM personas WHERE id = ? LIMIT 1'
    );
    $consulta->execute([$personaId]);
    $fila = $consulta->fetch();

    return $fila ? formatearPersona($fila) : null;
}

/**
 * Valida que un campo entero sea positivo.
 *
 * @param array<string, mixed> $datos
 */
function leerEnteroPositivo(array $datos, string $campo): int
{
    $valor = filter_var($datos[$campo] ?? null, FILTER_VALIDATE_INT);
    if ($valor === false || (int) $valor <= 0) {
        responder(false, 'El campo ' . $campo . ' debe ser un entero positivo', null, 400);
    }

    return (int) $valor;
}

/**
 * Normaliza y valida un UID RFID hexadecimal en mayúsculas (4 a 32 caracteres).
 */
function validarUidRfid(mixed $valor): string
{
    $uid = strtoupper(trim((string) $valor));
    if (preg_match('/^[0-9A-F]{4,32}$/', $uid) !== 1) {
        responder(false, 'El campo uid_rfid debe ser hexadecimal de 4 a 32 caracteres', null, 400);
    }

    return $uid;
}

if ($metodo === 'GET') {
    try {
        $consultaPersonas = $pdo->prepare(
            'SELECT id, nombre, uid_rfid, activo FROM personas ORDER BY id ASC'
        );
        $consultaPersonas->execute();
        $personas = [];
        foreach ($consultaPersonas->fetchAll() as $fila) {
            $personas[] = formatearPersona($fila);
        }

        $consultaPendientes = $pdo->prepare(
            'SELECT uid_rfid, dispositivo_id, fecha
             FROM tarjetas_pendientes
             ORDER BY fecha DESC, id DESC'
        );
        $consultaPendientes->execute();
        $pendientes = [];
        foreach ($consultaPendientes->fetchAll() as $fila) {
            $pendientes[] = [
                'uid_rfid' => (string) $fila['uid_rfid'],
                'dispositivo_id' => (int) $fila['dispositivo_id'],
                'fecha' => (string) $fila['fecha'],
            ];
        }

        responder(true, 'Personas y tarjetas pendientes obtenidas', [
            'personas' => $personas,
            'pendientes' => $pendientes,
        ]);
    } catch (PDOException $e) {
        error_log('Error SQL en GET app/personas.php: ' . $e->getMessage());
        responder(false, 'Error interno al consultar personas', null, 500);
    }
}

$datos = leerJson();
$accion = strtolower(trim((string) ($datos['accion'] ?? '')));

try {
    if ($accion === 'crear') {
        $nombre = trim((string) ($datos['nombre'] ?? ''));
        if ($nombre === '' || mb_strlen($nombre) > 100) {
            responder(false, 'El nombre de la persona es obligatorio y máximo de 100 caracteres', null, 400);
        }

        $uidCrudo = isset($datos['uid_rfid']) ? trim((string) $datos['uid_rfid']) : '';
        if ($uidCrudo !== '') {
            $uidRfid = validarUidRfid($uidCrudo);
            $consultaDuplicado = $pdo->prepare(
                'SELECT id FROM personas WHERE uid_rfid = ? LIMIT 1'
            );
            $consultaDuplicado->execute([$uidRfid]);
            if ($consultaDuplicado->fetch()) {
                responder(false, 'La tarjeta ya pertenece a otra persona', null, 409);
            }
        } else {
            // Marcador único mientras la persona no tiene tarjeta RFID física asignada.
            $uidRfid = 'SIN-' . strtoupper(bin2hex(random_bytes(8)));
        }

        $insertar = $pdo->prepare(
            'INSERT INTO personas (nombre, uid_rfid, activo) VALUES (?, ?, 1)'
        );
        $insertar->execute([$nombre, $uidRfid]);
        $personaId = (int) $pdo->lastInsertId();

        $persona = obtenerPersonaPorId($pdo, $personaId);
        responder(true, 'Persona creada', [
            'persona' => $persona,
        ], 201);
    }

    if ($accion === 'editar') {
        $personaId = leerEnteroPositivo($datos, 'id');
        $tieneNombre = array_key_exists('nombre', $datos);
        $tieneUid = array_key_exists('uid_rfid', $datos);

        if (!$tieneNombre && !$tieneUid) {
            responder(false, 'Debe indicar al menos nombre para editar', null, 400);
        }

        $actual = obtenerPersonaPorId($pdo, $personaId);
        if ($actual === null) {
            responder(false, 'Persona no encontrada', null, 404);
        }

        $campos = [];
        $valores = [];

        if ($tieneNombre) {
            $nombre = trim((string) $datos['nombre']);
            if ($nombre === '' || mb_strlen($nombre) > 100) {
                responder(false, 'El nombre de la persona es obligatorio y máximo de 100 caracteres', null, 400);
            }
            $campos[] = 'nombre = ?';
            $valores[] = $nombre;
        }

        if ($tieneUid) {
            $uidRfid = validarUidRfid($datos['uid_rfid']);
            $consultaDuplicado = $pdo->prepare(
                'SELECT id FROM personas WHERE uid_rfid = ? AND id <> ? LIMIT 1'
            );
            $consultaDuplicado->execute([$uidRfid, $personaId]);
            if ($consultaDuplicado->fetch()) {
                responder(false, 'La tarjeta ya pertenece a otra persona', null, 409);
            }
            $campos[] = 'uid_rfid = ?';
            $valores[] = $uidRfid;
        }

        $valores[] = $personaId;
        $actualizar = $pdo->prepare(
            'UPDATE personas SET ' . implode(', ', $campos) . ' WHERE id = ?'
        );
        $actualizar->execute($valores);

        $persona = obtenerPersonaPorId($pdo, $personaId);
        responder(true, 'Persona actualizada', [
            'persona' => $persona,
        ]);
    }

    if ($accion === 'desactivar' || $accion === 'activar') {
        $personaId = leerEnteroPositivo($datos, 'id');
        $actual = obtenerPersonaPorId($pdo, $personaId);
        if ($actual === null) {
            responder(false, 'Persona no encontrada', null, 404);
        }

        $activo = ($accion === 'activar') ? 1 : 0;
        $actualizar = $pdo->prepare('UPDATE personas SET activo = ? WHERE id = ?');
        $actualizar->execute([$activo, $personaId]);

        $persona = obtenerPersonaPorId($pdo, $personaId);
        $mensaje = ($activo === 1) ? 'Persona activada' : 'Persona desactivada';
        responder(true, $mensaje, [
            'persona' => $persona,
        ]);
    }

    if ($accion === 'asignar') {
        $uidRfid = validarUidRfid($datos['uid_rfid'] ?? '');
        $tienePersonaId = isset($datos['persona_id']) && $datos['persona_id'] !== '';
        $tieneNombre = isset($datos['nombre']) && trim((string) $datos['nombre']) !== '';

        if (!$tienePersonaId && !$tieneNombre) {
            responder(false, 'Debe indicar persona_id o nombre para asignar la tarjeta', null, 400);
        }

        $personaId = null;
        $nombre = '';

        if ($tienePersonaId) {
            $personaId = leerEnteroPositivo($datos, 'persona_id');
        } else {
            $nombre = trim((string) $datos['nombre']);
            if ($nombre === '' || mb_strlen($nombre) > 100) {
                responder(false, 'El nombre de la persona es obligatorio y máximo de 100 caracteres', null, 400);
            }
        }

        $pdo->beginTransaction();

        if ($personaId !== null) {
            $actual = obtenerPersonaPorId($pdo, $personaId);
            if ($actual === null) {
                $pdo->rollBack();
                responder(false, 'Persona no encontrada', null, 404);
            }

            $consultaDuplicado = $pdo->prepare(
                'SELECT id FROM personas WHERE uid_rfid = ? AND id <> ? LIMIT 1'
            );
            $consultaDuplicado->execute([$uidRfid, $personaId]);
            if ($consultaDuplicado->fetch()) {
                $pdo->rollBack();
                responder(false, 'La tarjeta ya pertenece a otra persona', null, 409);
            }

            $actualizar = $pdo->prepare('UPDATE personas SET uid_rfid = ? WHERE id = ?');
            $actualizar->execute([$uidRfid, $personaId]);
        } else {
            $consultaDuplicado = $pdo->prepare(
                'SELECT id FROM personas WHERE uid_rfid = ? LIMIT 1'
            );
            $consultaDuplicado->execute([$uidRfid]);
            if ($consultaDuplicado->fetch()) {
                $pdo->rollBack();
                responder(false, 'La tarjeta ya pertenece a otra persona', null, 409);
            }

            $insertar = $pdo->prepare(
                'INSERT INTO personas (nombre, uid_rfid, activo) VALUES (?, ?, 1)'
            );
            $insertar->execute([$nombre, $uidRfid]);
            $personaId = (int) $pdo->lastInsertId();
        }

        $eliminarPendiente = $pdo->prepare(
            'DELETE FROM tarjetas_pendientes WHERE uid_rfid = ?'
        );
        $eliminarPendiente->execute([$uidRfid]);

        $pdo->commit();

        $persona = obtenerPersonaPorId($pdo, $personaId);
        responder(true, 'Tarjeta asignada correctamente', [
            'persona' => $persona,
        ]);
    }

    responder(false, 'Acción no válida. Use crear, editar, desactivar, activar o asignar', null, 400);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ((string) $e->getCode() === '23000') {
        responder(false, 'La tarjeta ya pertenece a otra persona', null, 409);
    }
    error_log('Error SQL en POST app/personas.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar personas', null, 500);
} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Error inesperado en POST app/personas.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar personas', null, 500);
}
