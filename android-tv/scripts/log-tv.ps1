param([string]$Serial = "")
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$args = @()
if ($Serial) { $args += @("-s", $Serial) }
& $adb @args logcat -c
& $adb @args logcat "AndroidRuntime:E" "libvlc:V" "VLC:D" "*:S"
