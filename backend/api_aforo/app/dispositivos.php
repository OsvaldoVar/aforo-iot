<?php
// Gestión de dispositivos ESP32: listado con estado en línea y altas/edición/activación.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

const SEGUNDOS_EN_LINEA = 30;

$metodo = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if ($metodo !== 'GET' && $metodo !== 'POST') {
    header('Allow: GET, POST, OPTIONS');
    responder(false, 'Método HTTP no permitido', null, 405);
}

/**
 * Normaliza una fila de dispositivos al formato JSON de respuesta.
 *
 * @param array<string, mixed> $fila
 * @return array<string, mixed>
 */
function formatearDispositivo(array $fila): array
{
    return [
        'id' => (int) $fila['id'],
        'nombre' => (string) $fila['nombre'],
        'ubicacion' => $fila['ubicacion'] !== null ? (string) $fila['ubicacion'] : null,
        'api_key' => (string) $fila['api_key'],
        'activo' => ((int) $fila['activo']) === 1,
        'ultima_conexion' => $fila['ultima_conexion'] !== null ? (string) $fila['ultima_conexion'] : null,
        'en_linea' => ((int) $fila['en_linea']) === 1,
    ];
}

/**
 * Obtiene un dispositivo por su ID incluyendo el cálculo SQL de en_linea.
 *
 * @return array<string, mixed>|null
 */
function obtenerDispositivoPorId(PDO $pdo, int $dispositivoId): ?array
{
    $consulta = $pdo->prepare(
        'SELECT id, nombre, ubicacion, api_key, activo, ultima_conexion,
                CASE
                    WHEN ultima_conexion IS NOT NULL
                     AND TIMESTAMPDIFF(SECOND, ultima_conexion, NOW()) <= ?
                    THEN 1 ELSE 0
                END AS en_linea
         FROM dispositivos
         WHERE id = ?
         LIMIT 1'
    );
    $consulta->execute([SEGUNDOS_EN_LINEA, $dispositivoId]);
    $fila = $consulta->fetch();

    return $fila ? formatearDispositivo($fila) : null;
}

/**
 * Valida que el identificador de dispositivo sea un entero positivo.
 *
 * @param array<string, mixed> $datos
 */
function leerIdDispositivo(array $datos): int
{
    $id = filter_var($datos['id'] ?? null, FILTER_VALIDATE_INT);
    if ($id === false || (int) $id <= 0) {
        responder(false, 'El campo id debe ser un entero positivo', null, 400);
    }

    return (int) $id;
}

if ($metodo === 'GET') {
    try {
        // Calcular en_linea en SQL evita desfases de zona horaria entre PHP y MariaDB.
        $consulta = $pdo->prepare(
            'SELECT id, nombre, ubicacion, api_key, activo, ultima_conexion,
                    CASE
                        WHEN ultima_conexion IS NOT NULL
                         AND TIMESTAMPDIFF(SECOND, ultima_conexion, NOW()) <= ?
                        THEN 1 ELSE 0
                    END AS en_linea
             FROM dispositivos
             ORDER BY id ASC'
        );
        $consulta->execute([SEGUNDOS_EN_LINEA]);
        $filas = $consulta->fetchAll();

        $dispositivos = [];
        foreach ($filas as $fila) {
            $dispositivos[] = formatearDispositivo($fila);
        }

        responder(true, 'Dispositivos obtenidos', [
            'dispositivos' => $dispositivos,
        ]);
    } catch (PDOException $e) {
        error_log('Error SQL en GET app/dispositivos.php: ' . $e->getMessage());
        responder(false, 'Error interno al consultar los dispositivos', null, 500);
    }
}

$datos = leerJson();
$accion = strtolower(trim((string) ($datos['accion'] ?? '')));

try {
    if ($accion === 'crear') {
        $nombre = trim((string) ($datos['nombre'] ?? ''));
        if ($nombre === '' || mb_strlen($nombre) > 100) {
            responder(false, 'El nombre del dispositivo es obligatorio y máximo de 100 caracteres', null, 400);
        }

        $ubicacion = null;
        if (array_key_exists('ubicacion', $datos) && $datos['ubicacion'] !== null) {
            $ubicacionLimpia = trim((string) $datos['ubicacion']);
            if ($ubicacionLimpia !== '') {
                if (mb_strlen($ubicacionLimpia) > 150) {
                    responder(false, 'La ubicación debe tener máximo 150 caracteres', null, 400);
                }
                $ubicacion = $ubicacionLimpia;
            }
        }

        $apiKey = bin2hex(random_bytes(16));

        $pdo->beginTransaction();

        $insertarDispositivo = $pdo->prepare(
            'INSERT INTO dispositivos (nombre, ubicacion, api_key, activo) VALUES (?, ?, ?, 1)'
        );
        $insertarDispositivo->execute([$nombre, $ubicacion, $apiKey]);
        $dispositivoId = (int) $pdo->lastInsertId();

        $insertarLimite = $pdo->prepare(
            'INSERT INTO limites (dispositivo_id) VALUES (?)'
        );
        $insertarLimite->execute([$dispositivoId]);

        $pdo->commit();

        $dispositivo = obtenerDispositivoPorId($pdo, $dispositivoId);
        responder(true, 'Dispositivo creado', [
            'dispositivo' => $dispositivo,
        ], 201);
    }

    if ($accion === 'editar') {
        $dispositivoId = leerIdDispositivo($datos);
        $tieneNombre = array_key_exists('nombre', $datos);
        $tieneUbicacion = array_key_exists('ubicacion', $datos);

        if (!$tieneNombre && !$tieneUbicacion) {
            responder(false, 'Debe indicar al menos nombre o ubicacion para editar', null, 400);
        }

        $actual = obtenerDispositivoPorId($pdo, $dispositivoId);
        if ($actual === null) {
            responder(false, 'Dispositivo no encontrado', null, 404);
        }

        $campos = [];
        $valores = [];

        if ($tieneNombre) {
            $nombre = trim((string) $datos['nombre']);
            if ($nombre === '' || mb_strlen($nombre) > 100) {
                responder(false, 'El nombre del dispositivo es obligatorio y máximo de 100 caracteres', null, 400);
            }
            $campos[] = 'nombre = ?';
            $valores[] = $nombre;
        }

        if ($tieneUbicacion) {
            $ubicacion = null;
            if ($datos['ubicacion'] !== null) {
                $ubicacionLimpia = trim((string) $datos['ubicacion']);
                if ($ubicacionLimpia !== '') {
                    if (mb_strlen($ubicacionLimpia) > 150) {
                        responder(false, 'La ubicación debe tener máximo 150 caracteres', null, 400);
                    }
                    $ubicacion = $ubicacionLimpia;
                }
            }
            $campos[] = 'ubicacion = ?';
            $valores[] = $ubicacion;
        }

        $valores[] = $dispositivoId;
        $actualizar = $pdo->prepare(
            'UPDATE dispositivos SET ' . implode(', ', $campos) . ' WHERE id = ?'
        );
        $actualizar->execute($valores);

        $dispositivo = obtenerDispositivoPorId($pdo, $dispositivoId);
        responder(true, 'Dispositivo actualizado', [
            'dispositivo' => $dispositivo,
        ]);
    }

    if ($accion === 'desactivar' || $accion === 'activar') {
        $dispositivoId = leerIdDispositivo($datos);
        $actual = obtenerDispositivoPorId($pdo, $dispositivoId);
        if ($actual === null) {
            responder(false, 'Dispositivo no encontrado', null, 404);
        }

        $activo = ($accion === 'activar') ? 1 : 0;
        $actualizar = $pdo->prepare('UPDATE dispositivos SET activo = ? WHERE id = ?');
        $actualizar->execute([$activo, $dispositivoId]);

        $dispositivo = obtenerDispositivoPorId($pdo, $dispositivoId);
        $mensaje = ($activo === 1) ? 'Dispositivo activado' : 'Dispositivo desactivado';
        responder(true, $mensaje, [
            'dispositivo' => $dispositivo,
        ]);
    }

    responder(false, 'Acción no válida. Use crear, editar, desactivar o activar', null, 400);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Error SQL en POST app/dispositivos.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar el dispositivo', null, 500);
} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Error inesperado en POST app/dispositivos.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar el dispositivo', null, 500);
}
