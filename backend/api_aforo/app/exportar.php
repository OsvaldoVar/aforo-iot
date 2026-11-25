<?php
// Exportación a CSV de accesos o mediciones con filtros opcionales por dispositivo y rango de fechas.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

exigirMetodo('GET');

const LIMITE_MAXIMO = 10000;

/**
 * Valida una fecha estricta en formato YYYY-MM-DD.
 */
function parsearFechaDia(string $valor, string $nombreParam): DateTimeImmutable
{
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $valor)) {
        responder(false, "El parámetro {$nombreParam} debe tener formato YYYY-MM-DD", null, 400);
    }

    $fecha = DateTimeImmutable::createFromFormat('!Y-m-d', $valor);
    if ($fecha === false || $fecha->format('Y-m-d') !== $valor) {
        responder(false, "El parámetro {$nombreParam} no es una fecha válida", null, 400);
    }

    return $fecha;
}

$tipo = isset($_GET['tipo']) ? trim((string) $_GET['tipo']) : '';
if ($tipo !== 'accesos' && $tipo !== 'mediciones') {
    responder(false, 'El parámetro tipo es obligatorio y debe ser accesos o mediciones', null, 400);
}

$dispositivoIdParam = isset($_GET['dispositivo_id']) ? trim((string) $_GET['dispositivo_id']) : '';
$dispositivoId = null;

if ($dispositivoIdParam !== '') {
    if (!ctype_digit($dispositivoIdParam) || (int) $dispositivoIdParam <= 0) {
        responder(false, 'El parámetro dispositivo_id debe ser un entero positivo', null, 400);
    }
    $dispositivoId = (int) $dispositivoIdParam;
}

$desdeParam = isset($_GET['desde']) ? trim((string) $_GET['desde']) : '';
$hastaParam = isset($_GET['hasta']) ? trim((string) $_GET['hasta']) : '';
$fechaDesde = $desdeParam !== '' ? parsearFechaDia($desdeParam, 'desde') : null;
$fechaHasta = $hastaParam !== '' ? parsearFechaDia($hastaParam, 'hasta') : null;

if ($fechaDesde !== null && $fechaHasta !== null && $fechaDesde > $fechaHasta) {
    responder(false, 'El parámetro desde no puede ser mayor que hasta', null, 400);
}

$condiciones = [];
$parametros = [];

if ($tipo === 'accesos') {
    if ($dispositivoId !== null) {
        $condiciones[] = 'r.dispositivo_id = :dispositivo_id';
        $parametros[':dispositivo_id'] = [$dispositivoId, PDO::PARAM_INT];
    }

    // Comparación por rango semiabierto [desde, hasta + 1 día) para aprovechar el índice en fecha.
    if ($fechaDesde !== null) {
        $condiciones[] = 'r.fecha >= :desde';
        $parametros[':desde'] = [$fechaDesde->format('Y-m-d') . ' 00:00:00', PDO::PARAM_STR];
    }

    if ($fechaHasta !== null) {
        $hastaMasUnDia = $fechaHasta->modify('+1 day')->format('Y-m-d') . ' 00:00:00';
        $condiciones[] = 'r.fecha < :hasta_mas_1_dia';
        $parametros[':hasta_mas_1_dia'] = [$hastaMasUnDia, PDO::PARAM_STR];
    }

    $sql = 'SELECT r.id, r.fecha, p.nombre, r.tipo, r.dispositivo_id
            FROM registros_acceso r
            INNER JOIN personas p ON p.id = r.persona_id';

    if (!empty($condiciones)) {
        $sql .= ' WHERE ' . implode(' AND ', $condiciones);
    }

    $sql .= ' ORDER BY r.fecha ASC, r.id ASC LIMIT :limite';
    $encabezados = ['ID', 'Fecha', 'Nombre', 'Tipo', 'Dispositivo'];
} else {
    if ($dispositivoId !== null) {
        $condiciones[] = 'dispositivo_id = :dispositivo_id';
        $parametros[':dispositivo_id'] = [$dispositivoId, PDO::PARAM_INT];
    }

    // Comparación por rango semiabierto [desde, hasta + 1 día) para aprovechar el índice en fecha.
    if ($fechaDesde !== null) {
        $condiciones[] = 'fecha >= :desde';
        $parametros[':desde'] = [$fechaDesde->format('Y-m-d') . ' 00:00:00', PDO::PARAM_STR];
    }

    if ($fechaHasta !== null) {
        $hastaMasUnDia = $fechaHasta->modify('+1 day')->format('Y-m-d') . ' 00:00:00';
        $condiciones[] = 'fecha < :hasta_mas_1_dia';
        $parametros[':hasta_mas_1_dia'] = [$hastaMasUnDia, PDO::PARAM_STR];
    }

    $sql = 'SELECT id, fecha, temperatura, humedad, dispositivo_id FROM mediciones';

    if (!empty($condiciones)) {
        $sql .= ' WHERE ' . implode(' AND ', $condiciones);
    }

    $sql .= ' ORDER BY fecha ASC, id ASC LIMIT :limite';
    $encabezados = ['ID', 'Fecha', 'Temperatura', 'Humedad', 'Dispositivo'];
}

/**
 * Evita que Excel interprete como fórmula un texto que empieza con =, +, -, @ o tabulador.
 */
function celdaSegura(string $valor): string
{
    return preg_match('/^[=+\-@\t\r]/', $valor) ? "'" . $valor : $valor;
}

$desdeNombre = ($desdeParam !== '') ? $desdeParam : 'todo';
$hastaNombre = ($hastaParam !== '') ? $hastaParam : 'todo';
$nombreArchivo = sprintf('%s_%s_%s.csv', $tipo, $desdeNombre, $hastaNombre);

try {
    $consulta = $pdo->prepare($sql);

    foreach ($parametros as $clave => [$valor, $tipoParam]) {
        $consulta->bindValue($clave, $valor, $tipoParam);
    }
    $consulta->bindValue(':limite', LIMITE_MAXIMO, PDO::PARAM_INT);
    $consulta->execute();

    $filas = $consulta->fetchAll();

    header('Content-Type: text/csv; charset=utf-8');
    header('Content-Disposition: attachment; filename="' . $nombreArchivo . '"');

    $salida = fopen('php://output', 'w');
    if ($salida === false) {
        error_log('Error al abrir php://output en app/exportar.php');
        if (!headers_sent()) {
            responder(false, 'Error interno al generar el archivo CSV', null, 500);
        }
        exit;
    }

    fwrite($salida, "\xEF\xBB\xBF");
    fputcsv($salida, $encabezados, ',', '"', '');

    foreach ($filas as $fila) {
        if ($tipo === 'accesos') {
            $filaCsv = [
                $fila['id'],
                $fila['fecha'],
                celdaSegura((string) $fila['nombre']),
                $fila['tipo'],
                $fila['dispositivo_id'],
            ];
        } else {
            $filaCsv = [
                $fila['id'],
                $fila['fecha'],
                $fila['temperatura'],
                $fila['humedad'],
                $fila['dispositivo_id'],
            ];
        }
        fputcsv($salida, $filaCsv, ',', '"', '');
    }

    fclose($salida);
    exit;
} catch (PDOException $e) {
    error_log('Error SQL en app/exportar.php: ' . $e->getMessage());
    if (!headers_sent()) {
        responder(false, 'Error interno al exportar los datos', null, 500);
    }
    exit;
}
