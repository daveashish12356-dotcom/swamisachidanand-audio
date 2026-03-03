<?php
// bot_upload.php
//
// Simple API endpoint: Telegram bot se aaya suvichar text
// isko MySQL database ke 'suvichar' table me save karta hai.
//
// IMPORTANT:
// 1) Niche DB settings apne hosting ke hisaab se badalna.
// 2) Is file ko server par HTTPS URL se expose karo, e.g.:
//    https://yourwebsite.com/bot_upload.php

// ---------- 1. DB CONFIG (Yahan apni details daalo) ----------
$dbHost = "localhost";
$dbUser = "DB_USERNAME";   // TODO: MySQL username
$dbPass = "DB_PASSWORD";   // TODO: MySQL password
$dbName = "DB_NAME";       // TODO: Database name

// ---------- 2. DB CONNECT ----------
$conn = new mysqli($dbHost, $dbUser, $dbPass, $dbName);
if ($conn->connect_error) {
    http_response_code(500);
    echo "DB_ERROR";
    exit;
}

// ---------- 3. METHOD CHECK ----------
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo "METHOD_NOT_ALLOWED";
    exit;
}

// ---------- 4. INPUT ----------
$text = isset($_POST['text']) ? trim($_POST['text']) : '';

if ($text !== '') {
    $stmt = $conn->prepare("INSERT INTO suvichar (text) VALUES (?)");
    if ($stmt) {
        $stmt->bind_param("s", $text);
        $stmt->execute();
        $stmt->close();
        echo "Saved";
    } else {
        http_response_code(500);
        echo "PREPARE_FAILED";
    }
} else {
    echo "No Text";
}

$conn->close();

