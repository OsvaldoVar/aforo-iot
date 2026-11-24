<?php
// Recepción de mediciones de temperatura y humedad enviadas por el ESP32.

require_once __DIR__ . '/../conexion.php';

exigirMetodo('POST');

$datos = leerJson();
$apiKey = obtenerApiKey($datos);

if ($apiKey === '') {
    responder(false, 'Falta api_key del dispositivo', null, 401);
}

if (!isset($datos['temperatura'], $datos['humedad'])
    || !is_numeric($datos['temperatura'])
    || !is_numeric($datos['humedad'])
) {
    responder(false, 'Temperatura y humedad son obligatorias y deben ser numéricas', null, 400);
}

$temperatura = (float) $datos['temperatura'];
$humedad = (float) $datos['humedad'];

if (!is_finite($temperatura) || !is_finite($humedad)) {
    responder(false, 'Valores de temperatura o humedad inválidos', null, 400);
}

try {
    // El bloqueo por dispositivo serializa las alertas abiertas del mismo origen.
    $pdo->beginTransaction();
    $consulta = $pdo->prepare(
        'SELECT id FROM dispositivos WHERE api_key = ? AND activo = 1 LIMIT 1 FOR UPDATE'
    );
    $consulta->execute([$apiKey]);
    $dispositivo = $consulta->fetch();

    if (!$dispositivo) {
        $pdo->rollBack();
        responder(false, 'Dispositivo no autorizado o inactivo', null, 401);
    }

    $dispositivoId = (int) $dispositivo['id'];

    $pdo->prepare('UPDATE dispositivos SET ultima_conexion = NOW() WHERE id = ?')
        ->execute([$dispositivoId]);

    $insertar = $pdo->prepare(
        'INSERT INTO mediciones (dispositivo_id, temperatura, humedad) VALUES (?, ?, ?)'
    );
    $insertar->execute([$dispositivoId, $temperatura, $humedad]);
    $medicionId = (int) $pdo->lastInsertId();

    // S3-A2: reglas de alerta
    $consulta = $pdo->prepare('SELECT temp_max, hum_max FROM limites WHERE dispositivo_id = ?');
    $consulta->execute([$dispositivoId]);
    $limites = $consulta->fetch();
    $alerta = false;

    if ($limites) {
        $reglas = [
            ['temperatura', $temperatura, (float) $limites['temp_max'], 'Temperatura por encima del límite'],
            ['humedad', $humedad, (float) $limites['hum_max'], 'Humedad por encima del límite'],
        ];
        foreach ($reglas as [$tipo, $valor, $maximo, $mensaje]) {
            if ($valor <= $maximo) {
                continue;
            }
            $alerta = true;
            $consulta = $pdo->prepare(
                'SELECT id FROM alertas WHERE dispositivo_id = ? AND tipo = ? AND atendida = 0 LIMIT 1 FOR UPDATE'
            );
            $consulta->execute([$dispositivoId, $tipo]);
            if (!$consulta->fetch()) {
                $insertarAlerta = $pdo->prepare(
                    'INSERT INTO alertas (dispositivo_id, tipo, mensaje, valor) VALUES (?, ?, ?, ?)'
                );
                $insertarAlerta->execute([$dispositivoId, $tipo, $mensaje, $valor]);
            }
        }
    }

    $pdo->commit();

    responder(true, 'Medición registrada', [
        'medicion_id' => $medicionId,
        'dispositivo_id' => $dispositivoId,
        'temperatura' => $temperatura,
        'humedad' => $humedad,
        'alerta' => $alerta,
    ]);
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Error SQL en esp32/medicion.php: ' . $e->getMessage());
    responder(false, 'Error interno al guardar la medición', null, 500);
}
