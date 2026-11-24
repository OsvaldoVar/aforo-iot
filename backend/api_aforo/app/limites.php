<?php
// Consulta y configuración de los límites de clima y aforo por dispositivo.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

$metodo = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if (!in_array($metodo, ['GET', 'POST', 'PUT'], true)) {
    header('Allow: GET, POST, PUT, OPTIONS');
    responder(false, 'Método HTTP no permitido', null, 405);
}

$datos = $metodo === 'GET' ? $_GET : leerJson();
$dispositivoId = filter_var($datos['dispositivo_id'] ?? null, FILTER_VALIDATE_INT);
if (is_bool($datos['dispositivo_id'] ?? null) || $dispositivoId === false || $dispositivoId <= 0) {
    responder(false, 'El campo dispositivo_id debe ser un entero positivo', null, 400);
}

if ($metodo !== 'GET') {
    foreach (['temp_max' => 60, 'hum_max' => 100] as $campo => $maximo) {
        $valor = $datos[$campo] ?? null;
        if (!is_numeric($valor) || !is_finite((float) $valor) || $valor < 0 || $valor > $maximo) {
            $unidad = $campo === 'temp_max' ? '°C' : '%';
            responder(false, "El campo {$campo} debe ser numérico entre 0 y {$maximo} {$unidad}", null, 400);
        }
    }

    $aforoMax = filter_var($datos['aforo_max'] ?? null, FILTER_VALIDATE_INT);
    if (is_bool($datos['aforo_max'] ?? null) || $aforoMax === false || $aforoMax <= 0 || $aforoMax > 4294967295) {
        responder(false, 'El campo aforo_max debe ser un entero positivo hasta 4294967295', null, 400);
    }
}

try {
    $consulta = $pdo->prepare('SELECT id FROM dispositivos WHERE id = ? LIMIT 1');
    $consulta->execute([$dispositivoId]);
    if (!$consulta->fetch()) {
        responder(false, 'Dispositivo no encontrado', null, 404);
    }

    if ($metodo !== 'GET') {
        $guardar = $pdo->prepare(
            'INSERT INTO limites (dispositivo_id, temp_max, hum_max, aforo_max) VALUES (?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE temp_max = VALUES(temp_max), hum_max = VALUES(hum_max),
                                     aforo_max = VALUES(aforo_max)'
        );
        $guardar->execute([$dispositivoId, (float) $datos['temp_max'], (float) $datos['hum_max'], $aforoMax]);
    }

    $consulta = $pdo->prepare('SELECT dispositivo_id, temp_max, hum_max, aforo_max FROM limites WHERE dispositivo_id = ?');
    $consulta->execute([$dispositivoId]);
    $limites = $consulta->fetch();
    if (!$limites) {
        responder(false, 'Límites no encontrados para el dispositivo', null, 404);
    }

    responder(true, $metodo === 'GET' ? 'Límites obtenidos' : 'Límites guardados', [
        'limites' => [
            'dispositivo_id' => (int) $limites['dispositivo_id'],
            'temp_max' => (float) $limites['temp_max'],
            'hum_max' => (float) $limites['hum_max'],
            'aforo_max' => (int) $limites['aforo_max'],
        ],
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en app/limites.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar los límites', null, 500);
}
