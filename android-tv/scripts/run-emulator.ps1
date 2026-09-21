param([string]$AvdName = "RedPlay_TV_1080p")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"

if ((& $adb devices) -notmatch "emulator-\d+\s+device") {
    & (Join-Path $PSScriptRoot "start-emulator.ps1") $AvdName
}

& (Join-Path $PSScriptRoot "build-debug.ps1")

$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) { throw "Debug APK was not produced at $apk." }

$match = (& $adb devices) | Select-String "emulator-\d+\s+device" | Select-Object -First 1
if (-not $match) { throw "No booted Android emulator is available." }
$serial = $match.ToString().Split()[0]

Write-Host "Installing RedPlay on $serial..."
& $adb -s $serial install -r -d $apk
if ($LASTEXITCODE -ne 0) { throw "ADB install failed with exit code $LASTEXITCODE." }

& $adb -s $serial shell am force-stop com.redplay.iptv
& $adb -s $serial shell monkey -p com.redplay.iptv -c android.intent.category.LEANBACK_LAUNCHER 1
if ($LASTEXITCODE -ne 0) { throw "RedPlay launch failed with exit code $LASTEXITCODE." }

Write-Host "RedPlay updated and launched on $serial."
