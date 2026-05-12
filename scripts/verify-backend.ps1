param()

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleVersion = "8.8"
$toolsDir = Join-Path $repoRoot ".tools"
$gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleZip = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
$gradleExe = Join-Path $gradleHome "bin\\gradle.bat"
$gradleUserHome = Join-Path $toolsDir "gradle-user-home"
$apiDir = Join-Path $repoRoot "apps\\api"

function Assert-Command {
    param(
        [string]$Command,
        [string]$Message
    )

    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
        throw $Message
    }
}

Assert-Command -Command "java" -Message "Java 21+ no está disponible en PATH."

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

if (-not (Test-Path $gradleExe)) {
    Write-Host "Descargando Gradle $gradleVersion para validación repo-wide..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $gradleZip
    Expand-Archive -Path $gradleZip -DestinationPath $toolsDir -Force
}

$env:GRADLE_USER_HOME = $gradleUserHome

function Invoke-GradleTask {
    param([string]$Task)

    Write-Host ""
    Write-Host "==> Ejecutando $Task"
    & $gradleExe --no-daemon $Task
}

Push-Location $apiDir
try {
    $noisyDirs = @(
        (Join-Path $repoRoot "apps\\api\\sgf-app\\build\\test-results\\integrationTest"),
        (Join-Path $repoRoot "apps\\api\\sgf-integrations\\build\\test-results\\test"),
        (Join-Path $repoRoot "apps\\api\\sgf-ai\\build\\test-results\\test")
    )
    foreach ($dir in $noisyDirs) {
        if (Test-Path $dir) {
            Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
        }
    }

    Invoke-GradleTask ":sgf-pos:test"
    Invoke-GradleTask ":sgf-integrations:test"
    Invoke-GradleTask ":sgf-ai:test"
    Invoke-GradleTask ":sgf-app:test"
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "==> Ejecutando :sgf-app:integrationTest"
& (Join-Path $PSScriptRoot "test-infra.ps1") ":sgf-app:integrationTest"
