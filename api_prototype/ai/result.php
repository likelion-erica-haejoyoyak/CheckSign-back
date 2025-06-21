<?php
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
    if (!isset($_GET['image_id'])) {
        throw new Exception('Image ID not provided');
    }

    $imageId = $_GET['image_id'];
    
    $db = getDBConnection();
    $stmt = $db->prepare('SELECT status, result, error_message, request_time, complete_time FROM ai_analysis WHERE image_id = ?');
    $stmt->execute([$imageId]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        $db = null;
        http_response_code(404);
        echo json_encode(['success' => -1, 'error' => 'Analysis record not found']);
        exit;
    }
    
    $db = null;
    
    $response = [
        'success' => 1,
        'status' => $row['status'],
        'request_time' => $row['request_time']
    ];
    
    switch ($row['status']) {
        case 'pending':
            $response['message'] = 'Analysis in progress';
            break;
            
        case 'completed':
            $jsonData = json_decode($row['result'], true);
            $response['result'] = $jsonData;
            $response['complete_time'] = $row['complete_time'];
            break;
            
        case 'failed':
            $response['error_message'] = $row['error_message'];
            $response['complete_time'] = $row['complete_time'];
            break;
    }
    
    echo json_encode($response);

} catch (Exception $e) {
    echo json_encode(['success' => -1, 'error' => $e->getMessage()]);
}
?>