# Run this and add the SHA-1 to Google Cloud Console -> API Key -> Android apps
$debugKeystore = "$env:USERPROFILE\.android\debug.keystore"
if (-not (Test-Path $debugKeystore)) {
    Write-Host "Debug keystore not found at $debugKeystore"
    exit 1
}
Write-Host "Debug keystore SHA-1 (add to Google Cloud Console):"
Write-Host ""
& keytool -list -v -keystore $debugKeystore -alias androiddebugkey -storepass android -keypass android 2>$null | Select-String "SHA1:"
Write-Host ""
Write-Host "Package: com.swamisachidanand"
Write-Host "Add both to: APIs & Services -> Credentials -> Your API Key -> Application restrictions -> Android apps"
