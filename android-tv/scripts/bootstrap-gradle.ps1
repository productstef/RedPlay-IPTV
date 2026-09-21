param([switch]$Build)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$version = "8.13"
$cache = Join-Path $root ".gradle-bootstrap"
$gradleHome = Join-Path $cache "gradle-$version"
$zip = Join-Path $cache "gradle-$version-bin.zip"

# Gradle needs a JDK. Android Studio already ships one (JBR), so prefer it
# when JAVA_HOME/java are not configured globally on Windows.
$javaExe = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) { $javaExe = $candidate }
}
if (-not $javaExe) {
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { $javaExe = $javaCommand.Source }
}
if (-not $javaExe) {
    $studioJbrCandidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr")
    )
    foreach ($jbr in $studioJbrCandidates) {
        if ($jbr -and (Test-Path (Join-Path $jbr "bin\java.exe"))) {
            $env:JAVA_HOME = $jbr
            $env:Path = "$jbr\bin;$env:Path"
            $javaExe = Join-Path $jbr "bin\java.exe"
            break
        }
    }
}
if (-not $javaExe) {
    throw "Java/JDK was not found. Install Android Studio with its bundled JBR, or set JAVA_HOME."
}
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
