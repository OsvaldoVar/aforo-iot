<?php
// Gestión de comandos remotos: encolado desde la app y consulta de recientes.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';

$usuario = validarToken($pdo);

$metodo = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if ($metodo !== 'GET' && $metodo !== 'POST') {
    header('Allow: GET, POST, OPTIONS');
    responder(false, 'Método HTTP no permitido', null, 405);
}

if ($metodo === 'GET') {
    $dispositivoIdParam = isset($_GET['dispositivo_id']) ? trim((string) $_GET['dispositivo_id']) : '';
    $dispositivoId = null;

    if ($dispositivoIdParam !== '') {
        if (!ctype_digit($dispositivoIdParam) || (int) $dispositivoIdParam <= 0) {
            responder(false, 'El parámetro dispositivo_id debe ser un entero positivo', null, 400);
        }
        $dispositivoId = (int) $dispositivoIdParam;

        $check = $pdo->prepare('SELECT id FROM dispositivos WHERE id = ? LIMIT 1');
        $check->execute([$dispositivoId]);
        if (!$check->fetch()) {
            responder(false, 'Dispositivo no encontrado', null, 404);
        }
    }

    $limiteParam = isset($_GET['limite']) ? trim((string) $_GET['limite']) : '';
    $limite = 50;

    if ($limiteParam !== '') {
        if (!ctype_digit($limiteParam) || (int) $limiteParam <= 0) {
            responder(false, 'El parámetro limite debe ser un entero positivo', null, 400);
        }
        $limite = min((int) $limiteParam, 100);
    }

    try {
        if ($dispositivoId !== null) {
            $consulta = $pdo->prepare(
                'SELECT id, dispositivo_id, accion, estado, fecha_creacion, fecha_ejecucion
                 FROM comandos
                 WHERE dispositivo_id = ?
                 ORDER BY fecha_creacion DESC, id DESC
                 LIMIT ?'
            );
            $consulta->bindValue(1, $dispositivoId, PDO::PARAM_INT);
            $consulta->bindValue(2, $limite, PDO::PARAM_INT);
            $consulta->execute();
        } else {
            $consulta = $pdo->prepare(
                'SELECT id, dispositivo_id, accion, estado, fecha_creacion, fecha_ejecucion
                 FROM comandos
                 ORDER BY fecha_creacion DESC, id DESC
                 LIMIT ?'
            );
            $consulta->bindValue(1, $limite, PDO::PARAM_INT);
            $consulta->execute();
        }

        $filas = $consulta->fetchAll();
        $comandos = [];

        foreach ($filas as $fila) {
            $comandos[] = [
                'id' => (int) $fila['id'],
                'dispositivo_id' => (int) $fila['dispositivo_id'],
                'accion' => (string) $fila['accion'],
                'estado' => (string) $fila['estado'],
                'fecha_creacion' => (string) $fila['fecha_creacion'],
                'fecha_ejecucion' => $fila['fecha_ejecucion'] !== null ? (string) $fila['fecha_ejecucion'] : null,
            ];
        }

        responder(true, 'Comandos obtenidos', [
            'comandos' => $comandos,
            'total' => count($comandos),
        ]);
    } catch (PDOException $e) {
        error_log('Error SQL en GET app/comando.php: ' . $e->getMessage());
        responder(false, 'Error interno al consultar los comandos', null, 500);
    }
}

// POST: Encolar comando
$datos = leerJson();

$dispositivoIdRaw = $datos['dispositivo_id'] ?? $datos['id'] ?? null;
if ($dispositivoIdRaw === null || !is_numeric($dispositivoIdRaw) || (int) $dispositivoIdRaw <= 0) {
    responder(false, 'El campo dispositivo_id es obligatorio y debe ser un entero positivo', null, 400);
}
$dispositivoId = (int) $dispositivoIdRaw;

$accion = trim((string) ($datos['accion'] ?? $datos['comando'] ?? ''));
if ($accion === '') {
    responder(false, 'El campo accion es obligatorio', null, 400);
}

$accionesPermitidas = ['abrir_acceso', 'activar_alarma', 'alarma'];
if (!in_array($accion, $accionesPermitidas, true)) {
    responder(false, 'Acción no permitida. Solo se permite abrir_acceso o activar_alarma', null, 400);
}

try {
    $consulta = $pdo->prepare('SELECT id, activo FROM dispositivos WHERE id = ? LIMIT 1');
    $consulta->execute([$dispositivoId]);
    $dispositivo = $consulta->fetch();

    if (!$dispositivo) {
        responder(false, 'Dispositivo no encontrado', null, 404);
    }

    if ((int) $dispositivo['activo'] !== 1) {
        responder(false, 'El dispositivo está inactivo', null, 400);
    }

    $insertar = $pdo->prepare(
        'INSERT INTO comandos (dispositivo_id, accion, estado, fecha_creacion)
         VALUES (?, ?, \'pendiente\', NOW())'
    );
    $insertar->execute([$dispositivoId, $accion]);
    $comandoId = (int) $pdo->lastInsertId();

    $consulta = $pdo->prepare(
        'SELECT id, dispositivo_id, accion, estado, fecha_creacion, fecha_ejecucion
         FROM comandos
         WHERE id = ?
         LIMIT 1'
    );
    $consulta->execute([$comandoId]);
    $comando = $consulta->fetch();

    responder(true, 'Comando encolado exitosamente', [
        'comando' => [
            'id' => (int) $comando['id'],
            'dispositivo_id' => (int) $comando['dispositivo_id'],
            'accion' => (string) $comando['accion'],
            'estado' => (string) $comando['estado'],
            'fecha_creacion' => (string) $comando['fecha_creacion'],
            'fecha_ejecucion' => $comando['fecha_ejecucion'] !== null ? (string) $comando['fecha_ejecucion'] : null,
        ],
    ], 201);
} catch (PDOException $e) {
    error_log('Error SQL en POST app/comando.php: ' . $e->getMessage());
    responder(false, 'Error interno al encolar el comando', null, 500);
}
