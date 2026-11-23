<?php
// Validación del token de sesión (X-Token) para los endpoints de la app móvil.

/**
 * Valida el encabezado X-Token contra usuarios.token y devuelve el usuario autenticado.
 *
 * @return array{id: int, nombre: string, rol: string}
 */
function validarToken(PDO $pdo): array
{
    $token = obtenerEncabezado('X-Token');

    if ($token === '') {
        responder(false, 'Token inválido o ausente', null, 401);
    }

    try {
        $consulta = $pdo->prepare(
            'SELECT id, nombre, rol FROM usuarios WHERE token = ? LIMIT 1'
        );
        $consulta->execute([$token]);
        $usuario = $consulta->fetch();
    } catch (PDOException $e) {
        error_log('Error SQL en auth/validar_token.php: ' . $e->getMessage());
        responder(false, 'Error interno al validar el token', null, 500);
    }

    if (!$usuario) {
        responder(false, 'Token inválido o ausente', null, 401);
    }

    return [
        'id' => (int) $usuario['id'],
        'nombre' => (string) $usuario['nombre'],
        'rol' => (string) $usuario['rol'],
    ];
}
