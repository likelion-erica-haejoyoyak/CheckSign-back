# 프로토타입 API 구현 코드


## DB 스키마

### images 테이블

```sql
CREATE TABLE IF NOT EXISTS images (
    id VARCHAR(10) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    extension VARCHAR(10) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_hash (file_hash)
);
```

### ai_analysis 테이블

```sql
CREATE TABLE IF NOT EXISTS ai_analysis (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_id VARCHAR(10) NOT NULL,
    status ENUM('pending', 'completed', 'failed') DEFAULT 'pending',
    result TEXT NULL,
    error_message TEXT NULL,
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complete_time TIMESTAMP NULL,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE,
    UNIQUE KEY unique_image_analysis (image_id)
);
```

이 API는 계약서 이미지를 업로드하고 분석하는 서비스를 제공합니다. 다음은 주요 API 명세서입니다:

## API 명세서

### 이미지 관련 API
1. **이미지 업로드**
   - 엔드포인트: upload.php
   - 메서드: `POST`
   - 요청: `multipart/form-data` 형식으로 'image' 필드에 이미지 파일 첨부
   - 응답: `{ "success": 1, "id": "이미지ID" }` 또는 에러 메시지

2. **이미지 조회**
   - 엔드포인트: get.php
   - 메서드: `GET`
   - 파라미터: `id` (이미지 ID)
   - 응답: 이미지 파일 또는 에러 메시지

3. **이미지 삭제**
   - 엔드포인트: remove.php
   - 메서드: `GET`
   - 파라미터: `id` (이미지 ID)
   - 응답: `{ "success": 1 }` 또는 에러 메시지

### AI 분석 관련 API
1. **계약서 분석 요청**
   - 엔드포인트: request.php
   - 메서드: `POST`
   - 파라미터: `image_id` (분석할 이미지 ID)
   - 응답: `{ "success": 1, "message": "Analysis request submitted" }` 또는 에러 메시지

2. **분석 결과 조회**
   - 엔드포인트: result.php
   - 메서드: `GET`
   - 파라미터: `image_id` (이미지 ID)
   - 응답: 
     - 분석 중: `{ "success": 1, "status": "pending", "message": "Analysis in progress", "request_time": "요청시간" }`
     - 분석 완료: `{ "success": 1, "status": "completed", "result": { ... 분석결과 ... }, "request_time": "요청시간", "complete_time": "완료시간" }`
     - 분석 실패: `{ "success": 1, "status": "failed", "error_message": "에러메시지", "request_time": "요청시간", "complete_time": "완료시간" }`

### 분석 결과 형식
AI 분석 결과는 다음 JSON 형식으로 반환됨:
```
{
  "total_score": 0-100 사이 정수 (100이 가장 좋은 평가),
  "overview": "계약서 요약 내용 (한국어)",
  "terms_guide": "주요 법률 용어 설명 (한국어)",
  "risk_grade": 1-5 사이 숫자 (1이 가장 낮은 위험도, 5가 가장 높은 위험도)
}
```
