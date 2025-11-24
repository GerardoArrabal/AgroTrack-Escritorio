<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respond_error('Método no permitido', 405);
}

$usuarioId = isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : 0;

if ($usuarioId <= 0) {
    respond_error('usuario_id es obligatorio');
}

$db = get_db_connection();
$sql = 'SELECT FIN_ID, FIN_NOMBRE, FIN_UBICACION, FIN_SUPERFICIE, FIN_TIPO_SUELO, FIN_ESTADO, FIN_COORD_POLIGONO
        FROM finca
        WHERE FIN_USU_ID = ?
        ORDER BY FIN_NOMBRE ASC';
$stmt = $db->prepare($sql);
if (!$stmt) {
    respond_error('Error al preparar la consulta', 500);
}

$stmt->bind_param('i', $usuarioId);
$stmt->execute();
$result = $stmt->get_result();

$fincas = [];
while ($row = $result->fetch_assoc()) {
    $fincas[] = [
        'id' => (int)$row['FIN_ID'],
        'nombre' => $row['FIN_NOMBRE'],
        'ubicacion' => $row['FIN_UBICACION'],
        'superficie' => $row['FIN_SUPERFICIE'] !== null ? (float)$row['FIN_SUPERFICIE'] : null,
        'tipo_suelo' => $row['FIN_TIPO_SUELO'],
        'estado' => strtoupper($row['FIN_ESTADO']),
        'coordenadas' => json_decode($row['FIN_COORD_POLIGONO'], true) ?: $row['FIN_COORD_POLIGONO'],
    ];
}

$stmt->close();
$db->close();

respond([
    'status' => 'ok',
    'data' => [
        'fincas' => $fincas,
    ],
]);

