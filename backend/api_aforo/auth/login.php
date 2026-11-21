<?php
// Inicio de sesión y generación de token de acceso para la app móvil.

require_once __DIR__ . '/../conexion.php';

exigirMetodo('POST');

$datos = leerJson();
$correo = strtolower(trim((string) ($datos['correo'] ?? '')));
$password = (string) ($datos['password'] ?? '');

if ($correo === '' || $password === '') {
    responder(false, 'Faltan campos obligatorios: correo y password', null, 400);
}

try {
    $consulta = $pdo->prepare(
        'SELECT id, nombre, correo, password_hash, rol FROM usuarios WHERE correo = ? LIMIT 1'
    );
    $consulta->execute([$correo]);
    $usuario = $consulta->fetch();

    if (!$usuario || !password_verify($password, (string) $usuario['password_hash'])) {
        responder(false, 'Correo o contraseña incorrectos', null, 401);
    }

    $token = bin2hex(random_bytes(32));

    $actualizar = $pdo->prepare('UPDATE usuarios SET token = ? WHERE id = ?');
    $actualizar->execute([$token, $usuario['id']]);

    responder(true, 'Inicio de sesión exitoso', [
        'token' => $token,
        'nombre' => $usuario['nombre'],
        'rol' => $usuario['rol'],
        'usuario_id' => (int) $usuario['id'],
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en auth/login.php: ' . $e->getMessage());
    responder(false, 'Error interno al iniciar sesión', null, 500);
} catch (Exception $e) {
    error_log('Error al generar token en auth/login.php: ' . $e->getMessage());
    responder(false, 'Error interno al iniciar sesión', null, 500);
}
