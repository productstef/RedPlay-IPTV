param([string]$AvdName = "RedPlay_TV_1080p")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if ((& $adb devices) -notmatch "emulator-\d+\s+device") { & (Join-Path $PSScriptRoot "start-emulator.ps1") $AvdName }
& (Join-Path $PSScriptRoot "build-debug.ps1")
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$serial = ((& $adb devices) | Select-String "emulator-\d+\s+device" | Select-Object -First 1).ToString().Split()[0]
& $adb -s $serial install -r -d $apk
& $adb -s $serial shell am force-stop com.redplay.iptv
& $adb -s $serial shell monkey -p com.redplay.iptv -c android.intent.category.LEANBACK_LAUNCHER 1
