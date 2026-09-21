param([string]$Serial = "")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
& (Join-Path $PSScriptRoot "build-debug.ps1")
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$devices = @((& $adb devices) | Select-String "\sdevice$" | ForEach-Object { $_.ToString().Split()[0] } | Where-Object { $_ -notmatch '^emulator-' })
if (!$Serial) {
    if ($devices.Count -eq 0) { throw "No physical Android TV is connected. Run connect-tv.ps1 first." }
    if ($devices.Count -gt 1) { throw "More than one physical device is connected. Pass -Serial <device>." }
    $Serial = $devices[0]
}
& $adb -s $Serial install -r -d $apk
& $adb -s $Serial shell am force-stop com.redplay.iptv
& $adb -s $Serial shell monkey -p com.redplay.iptv -c android.intent.category.LEANBACK_LAUNCHER 1
Write-Host "RedPlay updated and launched on $Serial"
