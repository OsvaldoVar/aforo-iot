<?php
// Registro alternado de entradas y salidas por lectura RFID desde el ESP32.

require_once __DIR__ . '/../conexion.php';

exigirMetodo('POST');

$datos = leerJson();
$apiKey = obtenerApiKey($datos);
$uid = strtoupper(trim((string) ($datos['uid'] ?? '')));

if ($apiKey === '') {
    responder(false, 'Falta api_key del dispositivo', null, 401);
}

if ($uid === '') {
    responder(false, 'El UID de la tarjeta es obligatorio', null, 400);
}

try {
    // 1. Validar dispositivo activo y actualizar última conexión
    $consulta = $pdo->prepare(
        'SELECT id FROM dispositivos WHERE api_key = ? AND activo = 1 LIMIT 1'
    );
    $consulta->execute([$apiKey]);
    $dispositivo = $consulta->fetch();

    if (!$dispositivo) {
        responder(false, 'Dispositivo no autorizado o inactivo', null, 401);
    }

    $dispositivoId = (int) $dispositivo['id'];

    $pdo->prepare('UPDATE dispositivos SET ultima_conexion = NOW() WHERE id = ?')
        ->execute([$dispositivoId]);

    // 2. Buscar persona dueña de la tarjeta
    $consulta = $pdo->prepare(
        'SELECT id, nombre FROM personas WHERE uid_rfid = ? AND activo = 1 LIMIT 1'
    );
    $consulta->execute([$uid]);
    $persona = $consulta->fetch();

    if (!$persona) {
        $pendiente = $pdo->prepare(
            'INSERT INTO tarjetas_pendientes (uid_rfid, dispositivo_id, fecha)
             VALUES (?, ?, NOW())
             ON DUPLICATE KEY UPDATE dispositivo_id = ?, fecha = NOW()'
        );
        $pendiente->execute([$uid, $dispositivoId, $dispositivoId]);

        responder(false, 'Tarjeta no registrada', [
            'error' => 'tarjeta_no_registrada',
            'uid' => $uid,
        ]);
    }

    $personaId = (int) $persona['id'];

    // 3. Alternar entrada/salida según el último acceso en este dispositivo
    $consulta = $pdo->prepare(
        'SELECT tipo FROM registros_acceso
         WHERE persona_id = ? AND dispositivo_id = ?
         ORDER BY fecha DESC, id DESC
         LIMIT 1'
    );
    $consulta->execute([$personaId, $dispositivoId]);
    $ultimo = $consulta->fetchColumn();
    $tipo = ($ultimo === 'entrada') ? 'salida' : 'entrada';

    // S3-A2: negar entrada si se alcanzo aforo_max

    $insertar = $pdo->prepare(
        'INSERT INTO registros_acceso (persona_id, dispositivo_id, tipo) VALUES (?, ?, ?)'
    );
    $insertar->execute([$personaId, $dispositivoId, $tipo]);

    $consulta = $pdo->prepare(
        'SELECT COUNT(*) FROM personas_dentro WHERE dispositivo_id = ?'
    );
    $consulta->execute([$dispositivoId]);
    $dentro = (int) $consulta->fetchColumn();

    responder(true, ucfirst($tipo) . ' registrada', [
        'tipo' => $tipo,
        'nombre' => $persona['nombre'],
        'dentro' => $dentro,
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en esp32/acceso.php: ' . $e->getMessage());
    responder(false, 'Error interno al procesar el acceso', null, 500);
}
