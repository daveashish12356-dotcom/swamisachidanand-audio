Param()

$srcFolder = "C:\Users\davea\Desktop\શ્રી હનુમાન ચાલીસા"
$outDir = Join-Path (Get-Location) "audio-repo\hanuman_release"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# Sort files using first number in basename (same logic as Python helper)
$files = Get-ChildItem -Path $srcFolder -Filter *.mp3 | Sort-Object @{
    Expression = {
        if ($_.BaseName -match '\d+') {
            [int]$Matches[0]
        } else {
            999
        }
    }
}, Name

$i = 1
foreach ($f in $files) {
    $destName = "$i.mp3"
    $dest = Join-Path $outDir $destName
    Copy-Item -Path $f.FullName -Destination $dest -Force
    Write-Output ("{0}: {1} -> {2}" -f $i, $f.Name, $destName)
    $i++
}

Write-Output ("Release folder: " + $outDir)

