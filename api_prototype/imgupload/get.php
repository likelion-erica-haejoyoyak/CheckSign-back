<?php
// filepath: /var/www/html/imgupload/get.php

// CORS 헤더 추가
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// OPTIONS 요청 처리 (preflight)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../config.php';

try {
    if (!isset($_GET['id'])) {
        http_response_code(404);
        exit('Image ID not provided');
    }

    $imageId = $_GET['id'];
    
    $db = getDBConnection();
    $stmt = $db->prepare('SELECT filename, extension FROM images WHERE id = ?');
    $stmt->execute([$imageId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        $db = null;
        http_response_code(404);
        exit('Image not found');
    }
    
    $filename = $row['filename'];
    $extension = $row['extension'];
    $filePath = './images/' . $filename;
    
    $db = null;
    
    if (!file_exists($filePath)) {
        http_response_code(404);
        exit('Image file not found');
    }
    
    $mimeTypes = [
        'jpg' => 'image/jpeg',
        'jpeg' => 'image/jpeg',
        'png' => 'image/png',
        'gif' => 'image/gif',
        'webp' => 'image/webp'
    ];
    
    $mimeType = $mimeTypes[strtolower($extension)] ?? 'application/octet-stream';
    
    header('Content-Type: ' . $mimeType);
    header('Content-Length: ' . filesize($filePath));
    
    readfile($filePath);

} catch (Exception $e) {
    http_response_code(500);
    exit('Server error');
}
?>