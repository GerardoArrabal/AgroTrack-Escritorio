<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

const DB_HOST = '107.22.55.252';
const DB_PORT = 3306;
const DB_NAME = 'AgroTrack';
const DB_USER = 'admin';
const DB_PASSWORD = 'ebEQ8omv3gkV';

function respond(array $payload, int $httpCode = 200): void
{
    http_response_code($httpCode);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function respond_error(string $message, int $httpCode = 400): void
{
    respond([
        'status' => 'error',
        'message' => $message,
    ], $httpCode);
}

function get_db_connection(): mysqli
{
    $mysqli = @new mysqli(DB_HOST, DB_USER, DB_PASSWORD, DB_NAME, DB_PORT);
    if ($mysqli->connect_errno) {
        respond_error('Error al conectar con la base de datos', 500);
    }
    $mysqli->set_charset('utf8mb4');
    return $mysqli;
}

function get_json_input(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || $raw === '') {
        return [];
    }

    $decoded = json_decode($raw, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        respond_error('JSON inválido', 400);
    }

    return $decoded;
}

function sanitize_text(?string $value): string
{
    return trim($value ?? '');
}

