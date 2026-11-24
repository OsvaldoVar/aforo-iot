<?php
// Consulta de alertas y marcado de atención desde la app móvil.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

$metodo = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if ($metodo !== 'GET' && $metodo !== 'POST') {
    header('Allow: GET, POST, OPTIONS');
    responder(false, 'Método HTTP no permitido', null, 405);
}

try {
    if ($metodo === 'POST') {
        $datos = leerJson();
        $id = filter_var($datos['id'] ?? null, FILTER_VALIDATE_INT);
        if (is_bool($datos['id'] ?? null) || $id === false || $id <= 0) {
            responder(false, 'El campo id debe ser un entero positivo', null, 400);
        }

        $consulta = $pdo->prepare('SELECT id FROM alertas WHERE id = ? LIMIT 1');
        $consulta->execute([$id]);
        if (!$consulta->fetch()) {
            responder(false, 'Alerta no encontrada', null, 404);
        }

        $actualizar = $pdo->prepare('UPDATE alertas SET atendida = 1 WHERE id = ?');
        $actualizar->execute([$id]);
        responder(true, 'Alerta marcada como atendida', ['id' => $id, 'atendida' => true]);
    }

    $condiciones = [];
    $parametros = [];
    if (array_key_exists('atendida', $_GET)) {
        if (!in_array($_GET['atendida'], ['0', '1'], true)) {
            responder(false, 'El parámetro atendida debe ser 0 o 1', null, 400);
        }
        $condiciones[] = 'atendida = :atendida';
        $parametros[':atendida'] = (int) $_GET['atendida'];
    }

    if (array_key_exists('dispositivo_id', $_GET)) {
        $dispositivoId = filter_var($_GET['dispositivo_id'], FILTER_VALIDATE_INT);
        if ($dispositivoId === false || $dispositivoId <= 0) {
            responder(false, 'El parámetro dispositivo_id debe ser un entero positivo', null, 400);
        }
        $condiciones[] = 'dispositivo_id = :dispositivo_id';
        $parametros[':dispositivo_id'] = $dispositivoId;
    }

    $limite = filter_var($_GET['limite'] ?? $_GET['limit'] ?? 100, FILTER_VALIDATE_INT);
    if ($limite === false || $limite <= 0) {
        responder(false, 'El parámetro limite debe ser un entero positivo', null, 400);
    }
    $limite = min($limite, 1000);

    $sql = 'SELECT id, dispositivo_id, tipo, mensaje, valor, fecha, atendida FROM alertas';
    if ($condiciones !== []) {
        $sql .= ' WHERE ' . implode(' AND ', $condiciones);
    }
    $sql .= ' ORDER BY fecha DESC, id DESC LIMIT :limite';
    $consulta = $pdo->prepare($sql);
    foreach ($parametros as $clave => $valor) {
        $consulta->bindValue($clave, $valor, PDO::PARAM_INT);
    }
    $consulta->bindValue(':limite', $limite, PDO::PARAM_INT);
    $consulta->execute();

    $alertas = [];
    foreach ($consulta->fetchAll() as $fila) {
        $alertas[] = [
            'id' => (int) $fila['id'],
            'dispositivo_id' => (int) $fila['dispositivo_id'],
            'tipo' => (string) $fila['tipo'],
            'mensaje' => (string) $fila['mensaje'],
            'valor' => $fila['valor'] !== null ? (float) $fila['valor'] : null,
            'fecha' => (string) $fila['fecha'],
            'atendida' => (int) $fila['atendida'] === 1,
        ];
    }

    responder(true, 'Alertas obtenidas', ['alertas' => $alertas, 'total' => count($alertas)]);
} catch (PDOException $e) {
    error_log('Error SQL en app/alertas.php: ' . $e->getMessage());
    responder(false, 'Error interno al gestionar las alertas', null, 500);
}
