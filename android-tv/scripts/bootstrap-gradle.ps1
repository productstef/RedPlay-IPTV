param([switch]$Build)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$version = "8.13"
$cache = Join-Path $root ".gradle-bootstrap"
$home = Join-Path $cache "gradle-$version"
$zip = Join-Path $cache "gradle-$version-bin.zip"

if (!(Test-Path $home)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    Write-Host "Downloading Gradle $version..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $cache -Force
    Remove-Item $zip -Force
}

$gradle = Join-Path $home "bin\gradle.bat"
Push-Location $root
try {
    & $gradle wrapper --gradle-version $version --distribution-type bin
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Build) {
        & ".\gradlew.bat" :app:assembleDebug :app:testDebugUnitTest
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
Write-Host "Gradle wrapper ready. Open $root in Android Studio."
