$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (!(Test-Path (Join-Path $root "gradlew.bat"))) { & (Join-Path $PSScriptRoot "bootstrap-gradle.ps1") }
Push-Location $root
try { & ".\gradlew.bat" :app:assembleDebug; exit $LASTEXITCODE } finally { Pop-Location }
