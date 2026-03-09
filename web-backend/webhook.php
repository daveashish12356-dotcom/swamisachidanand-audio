<?php
@file_put_contents(__DIR__ . '/webhook_log.txt', date('Y-m-d H:i:s') . " START\n", FILE_APPEND);
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
$botToken    = "8752969355:AAHWvhzpjb_oW4RZMdZEwebXpkut7R0Czz8";
$ocrApiKey   = "K88518476488957";   // OCR.space key (ocr.space/ocrapi/freekey) - BOT TOKEN MAT DAALNA
$botUploadUrl = "https://ashish-dave.infinityfree.me/bot_upload.php";

$baseApi = "https://api.telegram.org/bot{$botToken}";

// ---------- 1.5 DEBUG: step-by-step log (webhook_log.txt me dekho kahin atak raha hai)
$logFile = __DIR__ . '/webhook_log.txt';
$log = function ($msg) use ($logFile) { @file_put_contents($logFile, date('Y-m-d H:i:s') . " " . $msg . "\n", FILE_APPEND); };
$log('1_received');

// ---------- 2. TELEGRAM UPDATE READ ----------
$raw    = file_get_contents("php://input");
$update = json_decode($raw, true);

if (!isset($update['message'])) {
    $log('2_no_message'); exit;
}
$message = $update['message'];

// Sirf photo messages handle karo
if (!isset($message['photo']) || !is_array($message['photo'])) {
    $log('2_no_photo'); exit;
}
$log('2_photo_ok');

// DEBUG: Bot reply bhejo - agar ye message aaye to webhook chal raha hai
$chatId = $message['chat']['id'] ?? 0;
if ($chatId) {
    @file_get_contents($baseApi . '/sendMessage?chat_id=' . $chatId . '&text=' . urlencode('Photo received! Processing...'));
}

// 1) sabse bada photo lo
$lastPhoto = end($message['photo']);
$fileId    = $lastPhoto['file_id'] ?? null;
if (!$fileId) { $log('2_no_file_id'); exit; }

// 2) Telegram se file path lo
$fileInfoUrl = "{$baseApi}/getFile?file_id={$fileId}";
$fileInfo    = @json_decode(@file_get_contents($fileInfoUrl), true);
if (empty($fileInfo['ok']) || empty($fileInfo['result']['file_path'])) {
    $log('3_getFile_fail'); exit;
}
$log('3_getFile_ok');

$filePath = $fileInfo['result']['file_path'];
$photoUrl = "https://api.telegram.org/file/bot{$botToken}/{$filePath}";

// ---------- 3. OCR API ko call ----------
$ocrApiUrl = "https://api.ocr.space/parse/imageurl?apikey={$ocrApiKey}&url=" . urlencode($photoUrl);
$ocrJson   = @file_get_contents($ocrApiUrl);
$ocrResult = @json_decode($ocrJson, true);
if (empty($ocrResult['ParsedResults'][0]['ParsedText'])) {
    $log('4_ocr_fail'); exit;
}
$log('4_ocr_ok');

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

@file_get_contents($botUploadUrl, false, $context);
$log('5_upload_done');
