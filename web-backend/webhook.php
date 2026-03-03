<?php
// webhook.php
//
// Telegram bot webhook:
// 1) User photo (suvichar image) bhejta hai.
// 2) Bot image ko OCR API ko bhejta hai.
// 3) OCR se nikla text aapki site ke bot_upload.php par POST hota hai.
//
// IMPORTANT:
// - $botToken, $ocrApiKey, $botUploadUrl ko apne hisaab se badalna.

// ---------- 1. CONFIG ----------
// NOTE: Security ke liye git repo me actual token NAHI rakhte.
// Deploy karte waqt server par isi file ki private copy me real values daalni hongi.
$botToken    = "YOUR_BOT_TOKEN_HERE";
$ocrApiKey   = "YOUR_OCR_API_KEY_HERE";   // e.g. OCR.space key
$botUploadUrl = "https://yourwebsite.com/bot_upload.php";

$baseApi = "https://api.telegram.org/bot{$botToken}";

// ---------- 2. TELEGRAM UPDATE READ ----------
$raw    = file_get_contents("php://input");
$update = json_decode($raw, true);

if (!isset($update['message'])) {
    exit;
}
$message = $update['message'];

// Sirf photo messages handle karo
if (!isset($message['photo']) || !is_array($message['photo'])) {
    exit;
}

// 1) sabse bada photo lo
$lastPhoto = end($message['photo']);
$fileId    = $lastPhoto['file_id'] ?? null;
if (!$fileId) exit;

// 2) Telegram se file path lo
$fileInfoUrl = "{$baseApi}/getFile?file_id={$fileId}";
$fileInfo    = json_decode(file_get_contents($fileInfoUrl), true);

if (empty($fileInfo['ok']) || empty($fileInfo['result']['file_path'])) {
    exit;
}

$filePath = $fileInfo['result']['file_path'];
$photoUrl = "https://api.telegram.org/file/bot{$botToken}/{$filePath}";

// ---------- 3. OCR API ko call ----------
$ocrApiUrl = "https://api.ocr.space/parse/imageurl?apikey={$ocrApiKey}&url=" . urlencode($photoUrl);
$ocrJson   = file_get_contents($ocrApiUrl);
$ocrResult = json_decode($ocrJson, true);

if (empty($ocrResult['ParsedResults'][0]['ParsedText'])) {
    exit;
}

$text = trim($ocrResult['ParsedResults'][0]['ParsedText']);
if ($text === '') {
    exit;
}

// ---------- 4. OCR text ko bot_upload.php par POST ----------
$postData = http_build_query(['text' => $text]);
$context  = stream_context_create([
    'http' => [
        'method'  => 'POST',
        'header'  => "Content-Type: application/x-www-form-urlencoded\r\n",
        'content' => $postData,
        'timeout' => 10,
    ],
]);

file_get_contents($botUploadUrl, false, $context);

