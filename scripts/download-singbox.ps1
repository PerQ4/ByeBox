# Download script for sing-box Android binaries
# Place this script in scripts/ and run from the project root.

param(
    [string]$Version = "1.10.7",
    [string]$OutputDir = "app/src/main/assets/sing-box"
)

$archMap = @{
    "arm64-v8a"   = "arm64-v8a"
    "armeabi-v7a" = "armeabi-v7a"
    "x86_64"      = "x86_64"
}

$baseUrl = "https://github.com/SagerNet/sing-box/releases/download/v$Version"

foreach ($abi in $archMap.Keys) {
    $filename = "sing-box-$Version-android-$abi.tar.gz"
    $url = "$baseUrl/$filename"
    $outDir = Join-Path $OutputDir $abi
    $outFile = Join-Path $env:TEMP $filename

    if (Test-Path (Join-Path $outDir "sing-box")) {
        Write-Host "sing-box for $abi already exists, skipping."
        continue
    }

    Write-Host "Downloading sing-box $Version for $abi..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $outFile -UseBasicParsing
        tar -xzf $outFile -C $env:TEMP
        $extracted = Join-Path $env:TEMP "sing-box-$Version-android-$abi" "sing-box"
        if (Test-Path $extracted) {
            Copy-Item $extracted (Join-Path $outDir "sing-box") -Force
            Write-Host "  -> Extracted to $outDir/sing-box"
        } else {
            Write-Host "  -> Binary not found in tarball, trying alternative structure..."
            $altExtracted = Join-Path $env:TEMP "sing-box-$Version-android-$abi" "bin" "sing-box"
            if (Test-Path $altExtracted) {
                Copy-Item $altExtracted (Join-Path $outDir "sing-box") -Force
                Write-Host "  -> Extracted to $outDir/sing-box"
            } else {
                Write-Host "  -> ERROR: Could not find binary in extracted files"
            }
        }
        Remove-Item $outFile -Force
    }
    catch {
        Write-Host "  -> ERROR downloading $abi`: $($_.Exception.Message)"
    }
}

Write-Host "Download complete."
