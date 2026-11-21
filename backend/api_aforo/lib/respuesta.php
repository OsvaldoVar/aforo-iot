<?php
// Utilidades comunes para respuestas JSON, CORS y lectura de peticiones.

ini_set('display_errors', '0');

/**
 * Envía cabeceras JSON/CORS y responde peticiones OPTIONS (preflight).
 */
function enviarCabeceras(): void
{
    date_default_timezone_set(defined('ZONA_HORARIA') ? ZONA_HORARIA : 'America/Mexico_City');
    $origen = defined('CORS_ORIGEN') ? CORS_ORIGEN : '*';

    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: ' . $origen);
    header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, X-Token, X-Api-Key');

    if (strtoupper($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
        http_response_code(204);
        exit;
    }
}

/**
 * Envía respuesta JSON con el formato del proyecto y detiene la ejecución.
 *
 * @param bool       $success Estado de la operación.
 * @param string     $mensaje Mensaje descriptivo para el cliente.
 * @param array|null $data    Campos adicionales a incluir en la respuesta.
 * @param int        $codigo  Código de estado HTTP.
 */
function responder(bool $success, string $mensaje, ?array $data = null, int $codigo = 200): void
{
    http_response_code($codigo);

    $respuesta = [
        'success' => $success,
        'mensaje' => $mensaje,
    ];

    if ($data !== null) {
        $respuesta = array_merge($respuesta, $data);
    }

    echo json_encode($respuesta, JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * Exige un método HTTP específico o responde 405.
 */
function exigirMetodo(string $metodo): void
{
    $esperado = strtoupper($metodo);
    $actual = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');

    if ($actual !== $esperado) {
        header('Allow: ' . $esperado . ', OPTIONS');
        responder(false, 'Método HTTP no permitido', null, 405);
    }
}

/**
 * Lee el cuerpo JSON de la petición con respaldo en $_POST.
 *
 * @return array<string, mixed>
 */
function leerJson(): array
{
    $cuerpo = file_get_contents('php://input');
    $cuerpo = ($cuerpo !== false) ? trim($cuerpo) : '';

    if ($cuerpo !== '') {
        $datos = json_decode($cuerpo, true);
        if (json_last_error() === JSON_ERROR_NONE && is_array($datos)) {
            return $datos;
        }

        if (!empty($_POST)) {
            return $_POST;
        }

        responder(false, 'Cuerpo JSON inválido', null, 400);
    }

    if (!empty($_POST)) {
        return $_POST;
    }

    return [];
}

/**
 * Obtiene un encabezado HTTP sin distinguir mayúsculas o minúsculas.
 */
function obtenerEncabezado(string $nombre): string
{
    $claveServer = 'HTTP_' . strtoupper(str_replace('-', '_', $nombre));
    if (isset($_SERVER[$claveServer])) {
        return trim((string) $_SERVER[$claveServer]);
    }

    if (function_exists('getallheaders')) {
        $encabezados = getallheaders();
        if (is_array($encabezados)) {
            foreach ($encabezados as $clave => $valor) {
                if (strcasecmp((string) $clave, $nombre) === 0) {
                    return trim((string) $valor);
                }
            }
        }
    }

    return '';
}

/**
 * Obtiene la api_key desde el cuerpo JSON o el encabezado X-Api-Key.
 *
 * @param array<string, mixed> $datos
 */
function obtenerApiKey(array $datos): string
{
    $apiKeyCuerpo = trim((string) ($datos['api_key'] ?? ''));
    if ($apiKeyCuerpo !== '') {
        return $apiKeyCuerpo;
    }

    return obtenerEncabezado('X-Api-Key');
}
