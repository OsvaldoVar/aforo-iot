<?php
// Carga de configuración local y conexión PDO a MySQL.

require_once __DIR__ . '/lib/respuesta.php';

$rutaConfig = __DIR__ . '/config.php';
$existeConfig = file_exists($rutaConfig);

if ($existeConfig) {
    require_once $rutaConfig;
}

enviarCabeceras();

if (!$existeConfig) {
    error_log('Falta el archivo de configuración: ' . $rutaConfig);
    responder(false, 'Error de configuración del servidor', null, 500);
}

if (!defined('DB_HOST') || !defined('DB_NAME') || !defined('DB_USER') || !defined('DB_PASS')) {
    error_log('Constantes de base de datos incompletas en config.php');
    responder(false, 'Error de configuración del servidor', null, 500);
}

$dbPort = defined('DB_PORT') ? (int) DB_PORT : 3306;
$dbCharset = defined('DB_CHARSET') ? DB_CHARSET : 'utf8mb4';

$dsn = sprintf(
    'mysql:host=%s;port=%d;dbname=%s;charset=%s',
    DB_HOST,
    $dbPort,
    DB_NAME,
    $dbCharset
);

try {
    $pdo = new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
} catch (PDOException $e) {
    error_log('Error de conexión PDO: ' . $e->getMessage());
    responder(false, 'No se pudo conectar con la base de datos', null, 500);
}
