<?php

// 데이터베이스 연결 설정
$db_host = 'localhost';
$db_user = '';              // 실제 DB 사용자 이름으로 변경
$db_pass = '';              // 실제 DB 비밀번호로 변경
$db_name = '';              // 실제 DB 이름으로 변경

function getDBConnection() {
    global $db_host, $db_user, $db_pass, $db_name;
    try {
        $db = new PDO("mysql:host=$db_host;dbname=$db_name;charset=utf8", $db_user, $db_pass);
        $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        return $db;
    } catch (PDOException $e) {
        die("Database connection failed: " . $e->getMessage());
    }
}
