<?php
// filepath: /var/www/html/imgupload/upload.php
// CORS 헤더 추가
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json');

// OPTIONS 요청 처리 (preflight)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../config.php';

function generateRandomId($length = 10) {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyz';
    $randomString = '';
    for ($i = 0; $i < $length; $i++) {
        $randomString .= $characters[rand(0, strlen($characters) - 1)];
    }
    return $randomString;
}

try {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Only POST method allowed');
    }

    if (!isset($_FILES['image'])) {
        throw new Exception('No image file uploaded');
    }

    $file = $_FILES['image'];
    
    if ($file['error'] !== UPLOAD_ERR_OK) {
        throw new Exception('File upload error');
    }

    $allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!in_array($file['type'], $allowedTypes)) {
        throw new Exception('Invalid file type');
    }

    // 파일 해시 계산
    $fileHash = hash_file('sha256', $file['tmp_name']);
    
    // 중복 파일 체크
    $db = getDBConnection();
    $checkStmt = $db->prepare('SELECT id FROM images WHERE file_hash = ?');
    $checkStmt->execute([$fileHash]);
    $existingFile = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existingFile) {
        $db = null;
        echo json_encode(['success' => 1, 'id' => $existingFile['id']]);
        exit;
    }

    $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
    $imageId = generateRandomId();
    $filename = $imageId . '.' . $extension;
    
    $uploadDir = './images/';
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }
    
    $filePath = $uploadDir . $filename;
    
    if (!move_uploaded_file($file['tmp_name'], $filePath)) {
        throw new Exception('Failed to move uploaded file');
    }

    $stmt = $db->prepare('INSERT INTO images (id, filename, extension, file_hash) VALUES (?, ?, ?, ?)');
    $stmt->execute([$imageId, $filename, $extension, $fileHash]);
    
    $db = null;
    
    echo json_encode(['success' => 1, 'id' => $imageId]);

} catch (Exception $e) {
    echo json_encode(['success' => -1, 'error' => $e->getMessage()]);
}
?>