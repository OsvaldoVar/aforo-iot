<?php
// Estado actual de un dispositivo: aforo dentro, límite, última medición y conexión.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

exigirMetodo('GET');

const SEGUNDOS_EN_LINEA = 30;

$dispositivoIdParam = isset($_GET['dispositivo_id']) ? trim((string) $_GET['dispositivo_id']) : '';
$dispositivoIdSolicitado = null;

if ($dispositivoIdParam !== '') {
    if (!ctype_digit($dispositivoIdParam) || (int) $dispositivoIdParam <= 0) {
        responder(false, 'El parámetro dispositivo_id debe ser un entero positivo', null, 400);
    }
    $dispositivoIdSolicitado = (int) $dispositivoIdParam;
}

try {
    // Calcular en_linea en SQL con TIMESTAMPDIFF evita desfases de zona horaria entre PHP y MySQL.
    if ($dispositivoIdSolicitado !== null) {
        $consultaDispositivo = $pdo->prepare(
            'SELECT id, nombre, ultima_conexion,
                    CASE
                        WHEN ultima_conexion IS NOT NULL
                         AND TIMESTAMPDIFF(SECOND, ultima_conexion, NOW()) <= ?
                        THEN 1 ELSE 0
                    END AS en_linea
             FROM dispositivos
             WHERE id = ? AND activo = 1
             LIMIT 1'
        );
        $consultaDispositivo->execute([SEGUNDOS_EN_LINEA, $dispositivoIdSolicitado]);
    } else {
        $consultaDispositivo = $pdo->prepare(
            'SELECT id, nombre, ultima_conexion,
                    CASE
                        WHEN ultima_conexion IS NOT NULL
                         AND TIMESTAMPDIFF(SECOND, ultima_conexion, NOW()) <= ?
                        THEN 1 ELSE 0
                    END AS en_linea
             FROM dispositivos
             WHERE activo = 1
             ORDER BY id ASC
             LIMIT 1'
        );
        $consultaDispositivo->execute([SEGUNDOS_EN_LINEA]);
    }

    $dispositivo = $consultaDispositivo->fetch();

    if (!$dispositivo) {
        responder(false, 'Dispositivo no encontrado', null, 404);
    }

    $dispositivoId = (int) $dispositivo['id'];

    $consultaDentro = $pdo->prepare(
        'SELECT COUNT(*) FROM personas_dentro WHERE dispositivo_id = ?'
    );
    $consultaDentro->execute([$dispositivoId]);
    $dentro = (int) $consultaDentro->fetchColumn();

    $consultaLimite = $pdo->prepare(
        'SELECT aforo_max FROM limites WHERE dispositivo_id = ? LIMIT 1'
    );
    $consultaLimite->execute([$dispositivoId]);
    $limite = $consultaLimite->fetchColumn();
    $aforoMax = ($limite !== false && $limite !== null) ? (int) $limite : null;

    $consultaMedicion = $pdo->prepare(
        'SELECT temperatura, humedad, fecha
         FROM mediciones
         WHERE dispositivo_id = ?
         ORDER BY fecha DESC, id DESC
         LIMIT 1'
    );
    $consultaMedicion->execute([$dispositivoId]);
    $filaMedicion = $consultaMedicion->fetch();

    $medicion = null;
    if ($filaMedicion) {
        $medicion = [
            'temperatura' => (float) $filaMedicion['temperatura'],
            'humedad' => (float) $filaMedicion['humedad'],
            'fecha' => (string) $filaMedicion['fecha'],
        ];
    }

    responder(true, 'Estado del dispositivo obtenido', [
        'dispositivo_id' => $dispositivoId,
        'nombre_dispositivo' => (string) $dispositivo['nombre'],
        'dentro' => $dentro,
        'aforo_max' => $aforoMax,
        'medicion' => $medicion,
        'en_linea' => ((int) $dispositivo['en_linea']) === 1,
        'ultima_conexion' => $dispositivo['ultima_conexion'] !== null
            ? (string) $dispositivo['ultima_conexion']
            : null,
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en app/estado.php: ' . $e->getMessage());
    responder(false, 'Error interno al consultar el estado', null, 500);
}
