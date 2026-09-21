param([switch]$Build)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$version = "8.13"
$cache = Join-Path $root ".gradle-bootstrap"
$gradleHome = Join-Path $cache "gradle-$version"
$zip = Join-Path $cache "gradle-$version-bin.zip"

# The current Android Studio JBR can be newer than Gradle/AGP supports.
# RedPlay's Android build is pinned to JDK 17 for predictable Windows + CI builds.
$jdkRoot = Join-Path $cache "jdk-17"
$javaExe = Get-ChildItem -Path $jdkRoot -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '\bin\java.exe$' } |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $javaExe) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $jdkZip = Join-Path $cache "jdk17-windows-x64.zip"
    Write-Host "Downloading JDK 17 for the RedPlay Android build..."
    Invoke-WebRequest "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile $jdkZip
    if (Test-Path $jdkRoot) { Remove-Item $jdkRoot -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $jdkRoot | Out-Null
    Expand-Archive -Path $jdkZip -DestinationPath $jdkRoot -Force
    Remove-Item $jdkZip -Force

    $javaExe = Get-ChildItem -Path $jdkRoot -Filter java.exe -Recurse |
        Where-Object { $_.FullName -match '\bin\java.exe$' } |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not $javaExe) {
    throw "JDK 17 bootstrap failed: java.exe was not found after extraction."
}

$javaBin = Split-Path -Parent $javaExe
$env:JAVA_HOME = Split-Path -Parent $javaBin
$env:Path = "$javaBin;$env:Path"

Write-Host "Using Java: $javaExe"
& $javaExe -version

if (!(Test-Path $gradleHome)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    Write-Host "Downloading Gradle $version..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $cache -Force
    Remove-Item $zip -Force
}

$gradle = Join-Path $gradleHome "bin\gradle.bat"
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
