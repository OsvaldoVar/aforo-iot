<?php
// Consulta y entrega atómica de comandos pendientes para el ESP32.

require_once __DIR__ . '/../conexion.php';

exigirMetodo('GET');

$apiKey = trim((string) ($_GET['api_key'] ?? ''));
if ($apiKey === '') {
    $apiKey = obtenerEncabezado('X-Api-Key');
}
if ($apiKey === '') {
    $cuerpo = file_get_contents('php://input');
    if ($cuerpo !== false && trim($cuerpo) !== '') {
        $datos = json_decode($cuerpo, true);
        if (is_array($datos)) {
            $apiKey = trim((string) ($datos['api_key'] ?? ''));
        }
    }
}

if ($apiKey === '') {
    responder(false, 'Falta api_key del dispositivo', null, 401);
}

try {
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

    $pdo->beginTransaction();

    $consultaComando = $pdo->prepare(
        'SELECT id, dispositivo_id, accion, estado, fecha_creacion, fecha_ejecucion
         FROM comandos
         WHERE dispositivo_id = ? AND estado = \'pendiente\'
         ORDER BY fecha_creacion ASC, id ASC
         LIMIT 1
         FOR UPDATE'
    );
    $consultaComando->execute([$dispositivoId]);
    $comando = $consultaComando->fetch();

    if (!$comando) {
        $pdo->commit();
        responder(true, 'No hay comandos pendientes', [
            'comando' => null,
        ]);
    }

    $comandoId = (int) $comando['id'];

    $actualizar = $pdo->prepare(
        'UPDATE comandos
         SET estado = \'ejecutado\', fecha_ejecucion = NOW()
         WHERE id = ?'
    );
    $actualizar->execute([$comandoId]);

    $consultaActualizado = $pdo->prepare(
        'SELECT id, dispositivo_id, accion, estado, fecha_creacion, fecha_ejecucion
         FROM comandos
         WHERE id = ?
         LIMIT 1'
    );
    $consultaActualizado->execute([$comandoId]);
    $comandoActualizado = $consultaActualizado->fetch();

    $pdo->commit();

    responder(true, 'Comando obtenido para ejecución', [
        'comando' => [
            'id' => (int) $comandoActualizado['id'],
            'dispositivo_id' => (int) $comandoActualizado['dispositivo_id'],
            'accion' => (string) $comandoActualizado['accion'],
            'estado' => (string) $comandoActualizado['estado'],
            'fecha_creacion' => (string) $comandoActualizado['fecha_creacion'],
            'fecha_ejecucion' => (string) $comandoActualizado['fecha_ejecucion'],
        ],
    ]);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Error SQL en esp32/comando.php: ' . $e->getMessage());
    responder(false, 'Error interno al consultar comandos', null, 500);
}
