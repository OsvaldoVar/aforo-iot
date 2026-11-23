<?php
// Registro de nuevos usuarios para la app móvil.

require_once __DIR__ . '/../conexion.php';

exigirMetodo('POST');

$datos = leerJson();
$nombre = trim((string) ($datos['nombre'] ?? ''));
$correo = strtolower(trim((string) ($datos['correo'] ?? '')));
$password = (string) ($datos['password'] ?? '');

if ($nombre === '' || $correo === '' || $password === '') {
    responder(false, 'Faltan campos obligatorios: nombre, correo y password', null, 400);
}

if (filter_var($correo, FILTER_VALIDATE_EMAIL) === false) {
    responder(false, 'El correo electrónico no es válido', null, 400);
}

if (strlen($password) < 6) {
    responder(false, 'La contraseña debe tener al menos 6 caracteres', null, 400);
}

try {
    $consulta = $pdo->prepare('SELECT id FROM usuarios WHERE correo = ? LIMIT 1');
    $consulta->execute([$correo]);

    if ($consulta->fetch()) {
        responder(false, 'El correo ya está registrado', null, 409);
    }

    $passwordHash = password_hash($password, PASSWORD_DEFAULT);

    $insertar = $pdo->prepare(
        'INSERT INTO usuarios (nombre, correo, password_hash) VALUES (?, ?, ?)'
    );
    $insertar->execute([$nombre, $correo, $passwordHash]);

    $usuarioId = (int) $pdo->lastInsertId();

    responder(true, 'Usuario registrado correctamente', [
        'usuario_id' => $usuarioId,
        'nombre' => $nombre,
        'correo' => $correo,
    ], 201);
} catch (PDOException $e) {
    if ($e->getCode() === '23000') {
        responder(false, 'El correo ya está registrado', null, 409);
    }

    error_log('Error en auth/registro.php: ' . $e->getMessage());
    responder(false, 'Error interno al registrar usuario', null, 500);
}
