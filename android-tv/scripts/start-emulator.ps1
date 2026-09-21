param(
    [string]$AvdName = "RedPlay_TV_1080p",
    [switch]$HardwareGraphics
)
$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$emulator = Join-Path $sdk "emulator\emulator.exe"
$adb = Join-Path $sdk "platform-tools\adb.exe"

if (!(Test-Path $emulator)) { throw "Android Emulator not found at $emulator. Install it from Android Studio > SDK Manager." }
if (!(Test-Path $adb)) { throw "adb not found at $adb. Install Android SDK Platform-Tools." }

$existing = & $emulator -list-avds
if ($existing -notcontains $AvdName) {
    throw "AVD '$AvdName' does not exist. Create a 1080p Android TV/Google TV AVD in Android Studio Device Manager, or pass its AVD name to this script."
}

$args = @("-avd", $AvdName, "-no-snapshot", "-no-boot-anim")
if (-not $HardwareGraphics) {
    # Stable inside VMware/nested virtualization. Use -HardwareGraphics to opt out.
    $args += @("-gpu", "software")
}

Start-Process $emulator -ArgumentList $args
& $adb wait-for-device

Write-Host "Waiting for Android to finish booting..."
do {
    Start-Sleep -Seconds 2
    $state = (& $adb get-state 2>$null).Trim()
    if ($state -ne "device") { continue }
    $boot = (& $adb shell getprop sys.boot_completed 2>$null).Trim()
} until ($boot -eq "1")

Write-Host "$AvdName is ready."
