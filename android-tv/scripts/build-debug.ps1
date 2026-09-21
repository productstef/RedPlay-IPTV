$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$bootstrap = Join-Path $PSScriptRoot "bootstrap-gradle.ps1"
$jdkRoot = Join-Path $root ".gradle-bootstrap\jdk-17"

function Set-RedPlayJava {
    $javaExe = Get-ChildItem -Path $jdkRoot -Filter java.exe -Recurse -File -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $javaExe) { return $false }

    $javaBin = Split-Path -Parent $javaExe
    $env:JAVA_HOME = Split-Path -Parent $javaBin
    if (($env:Path -split ';') -notcontains $javaBin) {
        $env:Path = "$javaBin;$env:Path"
    }
    Write-Host "Using Java: $javaExe"
    return $true
}

if (!(Test-Path (Join-Path $root "gradlew.bat")) -or !(Set-RedPlayJava)) {
    Write-Host "Preparing RedPlay JDK 17 / Gradle wrapper..."
    & $bootstrap
    if ($LASTEXITCODE -ne 0) { throw "RedPlay bootstrap failed with exit code $LASTEXITCODE." }
    if (!(Set-RedPlayJava)) { throw "JDK 17 was not found after bootstrap." }
}

Push-Location $root
try {
    & ".\gradlew.bat" :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Android debug build failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}
