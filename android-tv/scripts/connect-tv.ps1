param(
    [Parameter(Mandatory=$true)][string]$Address,
    [string]$PairCode = ""
)
$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (!(Test-Path $adb)) { throw "adb not found at $adb" }
if ($PairCode) {
    Write-Host "Pairing with $Address..."
    $PairCode | & $adb pair $Address
} else {
    Write-Host "Connecting to $Address..."
    & $adb connect $Address
}
& $adb devices -l
Write-Host "For Android 11+ Wireless debugging, pair once with: .\connect-tv.ps1 IP:PAIRING_PORT -PairCode 123456"
Write-Host "Then connect with: .\connect-tv.ps1 IP:ADB_PORT"
