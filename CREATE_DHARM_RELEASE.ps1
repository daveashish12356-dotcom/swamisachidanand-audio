# ધર્મ - Desktop folder -> MP3 -> GitHub release
# Run: cd f:\ss\audio-repo then .\CREATE_DHARM_RELEASE.ps1

param([string]$SourceFolder)

$ErrorActionPreference = "Stop"
$repo = "daveashish12356-dotcom/swamisachidanand-audio"
$tagName = "dharm"
$bookTitle = "ધર્મ"

# Use provided path or find folder with ~34 WAV files
if (-not $SourceFolder) {
    $desktop = [Environment]::GetFolderPath("Desktop")
    $folders = Get-ChildItem $desktop -Directory -ErrorAction SilentlyContinue
    foreach ($f in $folders) {
        $wavCount = (Get-ChildItem $f.FullName -Filter "*.wav" -ErrorAction SilentlyContinue).Count
        if ($wavCount -ge 30 -and $wavCount -le 40) {
            $SourceFolder = $f.FullName
            Write-Host "Found folder with $wavCount WAV files: $SourceFolder"
            break
        }
    }
}

if (-not $SourceFolder -or -not (Test-Path $SourceFolder)) {
    Write-Host "Folder nahi mila. Usage: .\CREATE_DHARM_RELEASE.ps1 -SourceFolder <path>"
    exit 1
}

$wavs = Get-ChildItem $SourceFolder -Filter "*.wav" -ErrorAction SilentlyContinue
$total = $wavs.Count
Write-Host "Source: $SourceFolder"
Write-Host "Total WAV: $total"
if ($total -eq 0) {
    Write-Host "Is folder me koi WAV nahi."
    exit 1
}

function Get-SortKey($name) {
    $base = [System.IO.Path]::GetFileNameWithoutExtension($name)
    $parts = $base -split "\.", 3
    $p1 = 0; $p2 = 0
    try { $p1 = [int]($parts[0] -replace "\D", "") } catch {}
    if ($parts.Length -ge 2 -and $parts[1] -match "(\d+)") { try { $p2 = [int]$matches[1] } catch {} }
    return $p1 * 100 + $p2
}
$wavs = $wavs | Sort-Object { Get-SortKey $_.Name }

$uploadDir = Join-Path $PSScriptRoot "dharm_mp3"
New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null
$partsJson = Join-Path $PSScriptRoot "dharm_parts.json"

$titles = @()
for ($i = 0; $i -lt $total; $i++) {
    $titles += [System.IO.Path]::GetFileNameWithoutExtension($wavs[$i].Name)
}

if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    Write-Host "ffmpeg nahi mila. Install: winget install ffmpeg"
    exit 1
}
Write-Host "Converting WAV to MP3..."
for ($i = 0; $i -lt $total; $i++) {
    $num = $i + 1
    $mp3Path = Join-Path $uploadDir "$num.mp3"
    Write-Host "[$num/$total] Converting -> $num.mp3"
    $null = cmd /c "ffmpeg -y -i `"$($wavs[$i].FullName)`" -codec:a libmp3lame -qscale:a 2 `"$mp3Path`" 2>nul"
    if (Test-Path $mp3Path) {
        Write-Host "  OK"
    } else {
        Write-Host "  FAIL"
    }
}
$mp3Count = (Get-ChildItem $uploadDir -Filter "*.mp3").Count
if ($mp3Count -lt $total) {
    Write-Host "Sirf $mp3Count MP3 bani. Expected: $total"
    exit 1
}
Write-Host "Conversion complete: $mp3Count MP3 files"

# Write parts JSON for audio_list
$sb = New-Object System.Text.StringBuilder
[void]$sb.Append("[")
for ($i = 0; $i -lt $total; $i++) {
    $num = $i + 1
    $t = $titles[$i].Replace([string][char]34, [string][char]92 + [string][char]34)
    [void]$sb.Append("{`"id`":`"$num`",`"title`":`"$t`",`"url`":`"https://github.com/daveashish12356-dotcom/swamisachidanand-audio/releases/download/$tagName/$num.mp3`"}")
    if ($i -lt ($total-1)) { [void]$sb.Append(",") }
}
[void]$sb.Append("]")
$sb.ToString() | Out-File -FilePath $partsJson -Encoding UTF8
Write-Host "Parts JSON saved: $partsJson"

# GitHub release
$ghExe = "C:\Program Files\GitHub CLI\gh.exe"
if (-not (Test-Path $ghExe)) { $ghExe = "gh" }
if (-not (Get-Command $ghExe -ErrorAction SilentlyContinue)) {
    Write-Host "gh nahi mila. Upload manually: $uploadDir ($total MP3) -> tag $tagName"
    Write-Host "After upload, run: node merge_dharm.js"
    exit 0
}
$files = (Get-ChildItem $uploadDir -Filter "*.mp3" | Sort-Object { [int]$_.BaseName }).FullName
Write-Host "Creating GitHub release $tagName..."
Push-Location $uploadDir
& $ghExe release create $tagName $files --repo $repo --title $bookTitle
Pop-Location
Write-Host "Release created. Next: Run node merge_dharm.js to update audio_list.json"
