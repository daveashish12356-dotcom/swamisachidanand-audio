# Prepare Chanakya Vyavaharniti for GitHub release
# Run: cd f:\ss   then   .\prepare_chanakya_release.ps1

$ErrorActionPreference = "Stop"
$repo = "daveashish12356-dotcom/swamisachidanand-audio"
$tagName = "chanakya_vyavaharniti"
$desktop = [Environment]::GetFolderPath("Desktop")
$sourceFolder = Join-Path $desktop "ચાણક્યની વ્યવહારનીતિ"

# Check source folder
if (-not (Test-Path $sourceFolder)) {
    Write-Host "Error: Source folder not found: $sourceFolder"
    exit 1
}

$wavs = Get-ChildItem $sourceFolder -Filter "*.wav" -ErrorAction SilentlyContinue
$total = $wavs.Count
Write-Host "Found $total WAV files in: $sourceFolder"

if ($total -eq 0) {
    Write-Host "Error: No WAV files found"
    exit 1
}

# Sort files by number
function Get-SortKey($name) {
    $base = [System.IO.Path]::GetFileNameWithoutExtension($name)
    $parts = $base -split '\.', 3
    $p1 = 0; $p2 = 0
    try { $p1 = [int]($parts[0] -replace '\D','') } catch {}
    if ($parts.Length -ge 2 -and $parts[1] -match '(\d+)') { try { $p2 = [int]$matches[1] } catch {} }
    return $p1 * 100 + $p2
}
$wavs = $wavs | Sort-Object { Get-SortKey $_.Name }

# Create upload directory
$uploadDir = Join-Path $PSScriptRoot "chanakya_upload"
New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null

# Copy files with simple names (1.wav, 2.wav, ...)
Write-Host ""
Write-Host "Copying files with simple names..."
for ($i = 0; $i -lt $total; $i++) {
    $num = $i + 1
    $destFile = Join-Path $uploadDir "$num.wav"
    Copy-Item $wavs[$i].FullName $destFile -Force
    Write-Host "  [$num/$total] $num.wav"
}

$copied = (Get-ChildItem $uploadDir -Filter "*.wav").Count
if ($copied -ne $total) {
    Write-Host "Error: Only $copied files copied, expected $total"
    exit 1
}

Write-Host ""
Write-Host "Files ready in: $uploadDir"
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Create thumbnail: Extract first page from PDF as JPG"
Write-Host "   Save as: f:\ss\public\thumbnails\chanakya_vyavaharniti.jpg"
Write-Host ""
Write-Host "2. Create GitHub release:"
Write-Host "   cd $uploadDir"
Write-Host "   gh release create $tagName *.wav --repo $repo --title 'Chanakya Vyavaharniti'"
Write-Host ""
Write-Host "   OR via browser:"
Write-Host "   - Go to: https://github.com/$repo/releases/new"
Write-Host "   - Tag: $tagName"
Write-Host "   - Upload all $total WAV files from: $uploadDir"
Write-Host ""
Write-Host "3. Push to GitHub:"
Write-Host "   git add public/audio_list.json public/thumbnails/chanakya_vyavaharniti.jpg"
Write-Host "   git commit -m 'Add Chanakya Vyavaharniti audio book'"
Write-Host "   git push"
