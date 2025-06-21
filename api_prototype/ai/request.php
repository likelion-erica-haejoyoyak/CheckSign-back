<?php
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

// Gemini API 설정
$GOOGLE_API_KEY = ''; // Gemini API 키

// 프롬프트 설정
$PROMPT = <<<EOD
You are an AI assistant specialized in legal document analysis. Your task is to thoroughly review, summarize, and evaluate a provided contract from a single image input. Based on the analysis of this image, generate a structured JSON response strictly adhering to the provided format:

{
  "total_score": integer (0 to 100, with 100 being the most favorable),
  "overview": string (written in Korean, summarizing clearly and concisely the type, key characteristics, and contractual relationship of the provided contract),
  "terms_guide": string (written in Korean, explaining key or potentially unfamiliar legal terms appearing in the contract, separate terms by line break),
  "risk_grade": number (1 to 5, with 1 indicating very low risk and 5 indicating very high risk)
}

Your evaluation criteria for the score and risk grade should include clarity of terms, fairness, potential legal liabilities, obligations, financial risks, and overall balance between contracting parties.

Important:

* If the contract provided is partially or entirely blank, clearly state that the contract contains empty fields but do NOT rate it as inherently risky solely due to blank fields.
* Risk grade should reflect the potential disadvantages, hidden problematic clauses, or subtly unfair terms that could negatively affect the contracting parties, not simply the existence of empty spaces.
* You can use bold style tag '<b></b>'. Use it to emphasize important points in your overview and terms guide.
* You MUST use <b> tags to highlight terms item. (ex: '<b>용어:</b> 용어 설명'), but at line break, do not use '<br>' but just use real line break. ('\\n')
* If there's any specified name in the contract (such as Company name, person name, etc), you must reiterate it in the overview.

Remember:

* The input will be provided as a single image containing the contract text.
* All textual outputs (`overview` and `terms_guide`) MUST be written in clear, professional Korean.
* Ensure that your explanations are precise, informative, and easy to understand for users who may not have extensive legal knowledge.
* Again, you should write the output in Korean(한국어) only.
* Blank fields in the contract should not be considered as a risk factor, but you should still mention that the contract contains empty fields.

Analyze the provided image carefully, and produce your structured response accordingly.
EOD;


function callGeminiAPI($imageData, $mimeType) {
    global $GOOGLE_API_KEY, $PROMPT;
    
    $url = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=' . $GOOGLE_API_KEY;
    
    $data = [
        'contents' => [
            [
                'role' => 'user',
                'parts' => [
                    [
                        'text' => $PROMPT
                    ],
                    [
                        'inline_data' => [
                            'mime_type' => $mimeType,
                            'data' => base64_encode($imageData)
                        ]
                    ]
                ]
            ]
        ],
        'generationConfig' => [
            'temperature' => 0.3,
            'thinkingConfig' => [
                'thinkingBudget' => -1,
            ],
            'responseMimeType' => 'application/json',
            'responseSchema' => [
                'type' => 'object',
                'properties' => [
                    'total_score' => [
                        'type' => 'integer'
                    ],
                    'overview' => [
                        'type' => 'string'
                    ],
                    'terms_guide' => [
                        'type' => 'string'
                    ],
                    'risk_grade' => [
                        'type' => 'number'
                    ]
                ],
                'required' => [
                    'total_score',
                    'overview',
                    'terms_guide',
                    'risk_grade'
                ]
            ]
        ]
    ];
    
    // CURL 사용하여 API 호출
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    
    $response = curl_exec($ch);
    
    if (curl_errno($ch)) {
        throw new Exception('CURL Error: ' . curl_error($ch));
    }
    
    curl_close($ch);
    
    return json_decode($response, true);
}

function processImageAnalysis($imageId) {
    global $GOOGLE_API_KEY;
    
    try {
        $db = getDBConnection();
        
        // 이미지 정보 조회
        $stmt = $db->prepare('SELECT filename, extension FROM images WHERE id = ?');
        $stmt->execute([$imageId]);
        $imageInfo = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$imageInfo) {
            throw new Exception('Image not found');
        }
        
        $imagePath = '../imgupload/images/' . $imageInfo['filename'];
        
        if (!file_exists($imagePath)) {
            throw new Exception('Image file not found');
        }
        
        // 이미지 데이터 읽기
        $imageData = file_get_contents($imagePath);
        $mimeTypes = [
            'jpg' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png' => 'image/png',
            'gif' => 'image/gif',
            'webp' => 'image/webp'
        ];
        $mimeType = $mimeTypes[strtolower($imageInfo['extension'])] ?? 'image/jpeg';
        
        // Gemini API 호출
        $response = callGeminiAPI($imageData, $mimeType);
        
        // API 응답 처리
        if (isset($response['candidates'][0]['content']['parts'][0]['text'])) {
            $result = $response['candidates'][0]['content']['parts'][0]['text'];
            
            // 성공 결과 저장
            $updateStmt = $db->prepare('UPDATE ai_analysis SET status = ?, result = ?, complete_time = NOW() WHERE image_id = ?');
            $updateStmt->execute(['completed', $result, $imageId]);
        } else if (isset($response['error'])) {
            // 에러가 발생한 경우 에러 정보를 저장
            throw new Exception(json_encode($response['error']));
        } else {
            // 응답 전체를 JSON으로 저장
            $updateStmt = $db->prepare('UPDATE ai_analysis SET status = ?, result = ?, complete_time = NOW() WHERE image_id = ?');
            $updateStmt->execute(['completed', json_encode($response), $imageId]);
        }
        
    } catch (Exception $e) {
        // 에러 결과 저장
        $db = getDBConnection();
        $updateStmt = $db->prepare('UPDATE ai_analysis SET status = ?, error_message = ?, complete_time = NOW() WHERE image_id = ?');
        $updateStmt->execute(['failed', $e->getMessage(), $imageId]);
    }
}

try {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Only POST method allowed');
    }
    
    if (!isset($_POST['image_id'])) {
        throw new Exception('Image ID not provided');
    }
    
    $imageId = $_POST['image_id'];
    
    $db = getDBConnection();
    
    // 이미지 존재 확인
    $checkStmt = $db->prepare('SELECT id FROM images WHERE id = ?');
    $checkStmt->execute([$imageId]);
    if (!$checkStmt->fetch()) {
        throw new Exception('Image not found');
    }
    
    // 이미 분석 요청이 있는지 확인
    $existingStmt = $db->prepare('SELECT status, result, error_message, request_time, complete_time FROM ai_analysis WHERE image_id = ?');
    $existingStmt->execute([$imageId]);
    $existing = $existingStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        if ($existing['status'] === 'pending') {
            echo json_encode(['success' => 1, 'message' => 'Analysis already in progress']);
            exit;
        } else if ($existing['status'] === 'completed') {
            // 이미 완료된 분석 결과가 있으면 즉시 반환
            echo json_encode([
                'success' => 1, 
                'message' => 'Analysis already completed',
                'status' => 'completed',
                'result' => $existing['result'],
                'request_time' => $existing['request_time'],
                'complete_time' => $existing['complete_time']
            ]);
            exit;
        } else if ($existing['status'] === 'failed') {
            // 실패한 기록이 있으면 상태를 pending으로 업데이트
            $updateStmt = $db->prepare('UPDATE ai_analysis SET status = ?, error_message = NULL, complete_time = NULL, request_time = NOW() WHERE image_id = ?');
            $updateStmt->execute(['pending', $imageId]);
        }
    } else {
        // 새로운 분석 요청 레코드 생성
        $insertStmt = $db->prepare('INSERT INTO ai_analysis (image_id, status) VALUES (?, ?)');
        $insertStmt->execute([$imageId, 'pending']);
    }
    
    $db = null;
    
    // 응답 즉시 반환
    echo json_encode(['success' => 1, 'message' => 'Analysis request submitted']);
    
    // 출력 버퍼 플러시
    if (ob_get_level()) {
        ob_end_flush();
    }
    flush();
    
    // 백그라운드에서 API 호출 처리
    processImageAnalysis($imageId);
    
} catch (Exception $e) {
    echo json_encode(['success' => -1, 'error' => $e->getMessage()]);
}
?>
