<?php
// Listado de personas actualmente dentro según la vista personas_dentro.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

exigirMetodo('GET');

$dispositivoIdParam = isset($_GET['dispositivo_id']) ? trim((string) $_GET['dispositivo_id']) : '';
$dispositivoId = null;

if ($dispositivoIdParam !== '') {
    if (!ctype_digit($dispositivoIdParam) || (int) $dispositivoIdParam <= 0) {
        responder(false, 'El parámetro dispositivo_id debe ser un entero positivo', null, 400);
    }
    $dispositivoId = (int) $dispositivoIdParam;
}

try {
    if ($dispositivoId !== null) {
        $consulta = $pdo->prepare(
            'SELECT id, nombre, hora_entrada
             FROM personas_dentro
             WHERE dispositivo_id = ?
             ORDER BY hora_entrada DESC, id DESC'
        );
        $consulta->execute([$dispositivoId]);
    } else {
        $consulta = $pdo->prepare(
            'SELECT id, nombre, hora_entrada
             FROM personas_dentro
             ORDER BY hora_entrada DESC, id DESC'
        );
        $consulta->execute();
    }

    $filas = $consulta->fetchAll();
    $personas = [];

    foreach ($filas as $fila) {
        $personas[] = [
            'id' => (int) $fila['id'],
            'nombre' => (string) $fila['nombre'],
            'hora_entrada' => (string) $fila['hora_entrada'],
        ];
    }

    responder(true, 'Personas dentro obtenidas', [
        'personas' => $personas,
        'total' => count($personas),
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en app/dentro.php: ' . $e->getMessage());
    responder(false, 'Error interno al consultar personas dentro', null, 500);
}
