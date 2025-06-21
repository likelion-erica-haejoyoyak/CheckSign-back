<?php
// filepath: /var/www/html/imgupload/remove.php

// CORS 헤더 추가
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json');

// OPTIONS 요청 처리 (preflight)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once '../config.php';

try {
    if (!isset($_GET['id'])) {
        throw new Exception('Image ID not provided');
    }

    $imageId = $_GET['id'];
    
    $db = getDBConnection();
    $stmt = $db->prepare('SELECT filename FROM images WHERE id = ?');
    $stmt->execute([$imageId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        $db = null;
        throw new Exception('Image not found');
    }
    
    $filename = $row['filename'];
    $filePath = './images/' . $filename;
    
    $deleteStmt = $db->prepare('DELETE FROM images WHERE id = ?');
    $deleteStmt->execute([$imageId]);

    $deleteAnalysisStmt = $db->prepare('DELETE FROM ai_analysis WHERE image_id = ?');
    $deleteAnalysisStmt->execute([$imageId]);
    
    $db = null;
    
    if (file_exists($filePath)) {
        unlink($filePath);
    }
    
    echo json_encode(['success' => 1]);

} catch (Exception $e) {
    echo json_encode(['success' => -1, 'error' => $e->getMessage()]);
}
?>