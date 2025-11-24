<?php
declare(strict_types=1);

require_once __DIR__ . '/config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond_error('Método no permitido', 405);
}

$input = get_json_input();
$usuario = sanitize_text($input['usuario'] ?? '');
$password = $input['password'] ?? '';

if ($usuario === '' || $password === '') {
    respond_error('Usuario y contraseña son obligatorios');
}

$db = get_db_connection();
$sql = 'SELECT USU_ID, USU_NOMBRE, USU_APELLIDOS, USU_EMAIL, USU_USERNAME, USU_PASSWORD, USU_ROL
        FROM usuario
        WHERE (USU_USERNAME = ? OR USU_EMAIL = ?) AND USU_ACTIVO = 1
        LIMIT 1';
$stmt = $db->prepare($sql);
if (!$stmt) {
    respond_error('Error al preparar la consulta', 500);
}

$stmt->bind_param('ss', $usuario, $usuario);
$stmt->execute();
$result = $stmt->get_result();
$datosUsuario = $result->fetch_assoc();
$stmt->close();
$db->close();

if (!$datosUsuario) {
    respond_error('Credenciales inválidas', 401);
}

$hash = $datosUsuario['USU_PASSWORD'];
if (!password_verify($password, $hash) && $password !== $hash) {
    respond_error('Credenciales inválidas', 401);
}

respond([
    'status' => 'ok',
    'data' => [
        'usuario' => [
            'id' => (int)$datosUsuario['USU_ID'],
            'nombre' => $datosUsuario['USU_NOMBRE'],
            'apellidos' => $datosUsuario['USU_APELLIDOS'],
            'email' => $datosUsuario['USU_EMAIL'],
            'username' => $datosUsuario['USU_USERNAME'],
            'rol' => strtoupper($datosUsuario['USU_ROL']),
        ],
    ],
]);

