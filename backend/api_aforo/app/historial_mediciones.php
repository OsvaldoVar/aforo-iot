<?php
// Historial de mediciones ambientales con filtros por dispositivo, fechas, límite y orden para gráficas.

require_once __DIR__ . '/../conexion.php';
require_once __DIR__ . '/../auth/validar_token.php';
$usuario = validarToken($pdo);

exigirMetodo('GET');

const LIMITE_DEFECTO = 100;
const LIMITE_MAXIMO = 1000;

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

$limiteParam = isset($_GET['limite']) ? trim((string) $_GET['limite']) : '';
$limite = LIMITE_DEFECTO;

if ($limiteParam !== '') {
    if (!ctype_digit($limiteParam) || (int) $limiteParam <= 0) {
        responder(false, 'El parámetro limite debe ser un entero positivo', null, 400);
    }
    $limite = min((int) $limiteParam, LIMITE_MAXIMO);
}

$ordenParam = isset($_GET['orden']) ? strtolower(trim((string) $_GET['orden'])) : '';
if ($ordenParam !== '' && $ordenParam !== 'asc' && $ordenParam !== 'desc') {
    responder(false, 'El parámetro orden debe ser asc o desc', null, 400);
}
$direccionOrden = ($ordenParam === 'asc') ? 'ASC' : 'DESC';

$condiciones = [];
$parametros = [];

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

$sql = 'SELECT id, temperatura, humedad, fecha, dispositivo_id FROM mediciones';

if (!empty($condiciones)) {
    $sql .= ' WHERE ' . implode(' AND ', $condiciones);
}

$sql .= " ORDER BY fecha {$direccionOrden}, id {$direccionOrden} LIMIT :limite";

try {
    $consulta = $pdo->prepare($sql);

    foreach ($parametros as $clave => [$valor, $tipo]) {
        $consulta->bindValue($clave, $valor, $tipo);
    }
    $consulta->bindValue(':limite', $limite, PDO::PARAM_INT);
    $consulta->execute();

    $filas = $consulta->fetchAll();
    $mediciones = [];

    foreach ($filas as $fila) {
        $mediciones[] = [
            'id' => (int) $fila['id'],
            'temperatura' => (float) $fila['temperatura'],
            'humedad' => (float) $fila['humedad'],
            'fecha' => (string) $fila['fecha'],
            'dispositivo_id' => (int) $fila['dispositivo_id'],
        ];
    }

    responder(true, 'Historial de mediciones obtenido', [
        'mediciones' => $mediciones,
        'total' => count($mediciones),
    ]);
} catch (PDOException $e) {
    error_log('Error SQL en app/historial_mediciones.php: ' . $e->getMessage());
    responder(false, 'Error interno al consultar el historial de mediciones', null, 500);
}
