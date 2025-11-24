<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respond_error('Método no permitido', 405);
}

$fincaId = isset($_GET['finca_id']) ? (int)$_GET['finca_id'] : 0;
$usuarioId = isset($_GET['usuario_id']) ? (int)$_GET['usuario_id'] : 0;

if ($fincaId <= 0 || $usuarioId <= 0) {
    respond_error('finca_id y usuario_id son obligatorios');
}

$db = get_db_connection();

$sqlFinca = 'SELECT FIN_ID, FIN_USU_ID, FIN_NOMBRE, FIN_UBICACION, FIN_SUPERFICIE, FIN_TIPO_SUELO,
                    FIN_COORD_POLIGONO, FIN_SISTEMA_RIEGO, FIN_ESTADO, FIN_FECHA_REGISTRO
             FROM finca
             WHERE FIN_ID = ? AND FIN_USU_ID = ?
             LIMIT 1';
$stmt = $db->prepare($sqlFinca);
if (!$stmt) {
    respond_error('Error al preparar la consulta', 500);
}
$stmt->bind_param('ii', $fincaId, $usuarioId);
$stmt->execute();
$fincaResult = $stmt->get_result();
$finca = $fincaResult->fetch_assoc();
$stmt->close();

if (!$finca) {
    $db->close();
    respond_error('Finca no encontrada', 404);
}

$sqlCultivos = 'SELECT CUL_ID, CUL_NOMBRE, CUL_VARIEDAD, CUL_FECHA_SIEMBRA, CUL_FECHA_COSECHA,
                       CUL_ESTADO, CUL_PRODUCCION_KG, CUL_REND_ESTIMADO, CUL_REND_REAL
                FROM cultivo
                WHERE CUL_FIN_ID = ?
                ORDER BY CUL_FECHA_SIEMBRA DESC';
$stmtCultivos = $db->prepare($sqlCultivos);
if (!$stmtCultivos) {
    $db->close();
    respond_error('Error al preparar cultivos', 500);
}
$stmtCultivos->bind_param('i', $fincaId);
$stmtCultivos->execute();
$cultivosResult = $stmtCultivos->get_result();

$cultivos = [];
while ($row = $cultivosResult->fetch_assoc()) {
    $cultivos[] = [
        'id' => (int)$row['CUL_ID'],
        'nombre' => $row['CUL_NOMBRE'],
        'variedad' => $row['CUL_VARIEDAD'],
        'fecha_siembra' => $row['CUL_FECHA_SIEMBRA'],
        'fecha_cosecha' => $row['CUL_FECHA_COSECHA'],
        'estado' => strtoupper($row['CUL_ESTADO']),
        'produccion_kg' => $row['CUL_PRODUCCION_KG'] !== null ? (float)$row['CUL_PRODUCCION_KG'] : null,
        'rendimiento_estimado' => $row['CUL_REND_ESTIMADO'] !== null ? (float)$row['CUL_REND_ESTIMADO'] : null,
        'rendimiento_real' => $row['CUL_REND_REAL'] !== null ? (float)$row['CUL_REND_REAL'] : null,
    ];
}
$stmtCultivos->close();
$db->close();

$responseFinca = [
    'id' => (int)$finca['FIN_ID'],
    'nombre' => $finca['FIN_NOMBRE'],
    'ubicacion' => $finca['FIN_UBICACION'],
    'superficie' => $finca['FIN_SUPERFICIE'] !== null ? (float)$finca['FIN_SUPERFICIE'] : null,
    'tipo_suelo' => $finca['FIN_TIPO_SUELO'],
    'estado' => strtoupper($finca['FIN_ESTADO']),
    'coordenadas' => json_decode($finca['FIN_COORD_POLIGONO'], true) ?: $finca['FIN_COORD_POLIGONO'],
    'sistema_riego' => $finca['FIN_SISTEMA_RIEGO'],
    'fecha_registro' => $finca['FIN_FECHA_REGISTRO'],
];

respond([
    'status' => 'ok',
    'data' => [
        'finca' => $responseFinca,
        'cultivos' => $cultivos,
    ],
]);

