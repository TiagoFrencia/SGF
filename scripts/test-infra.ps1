param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradleVersion = "8.8"
$toolsDir = Join-Path $repoRoot ".tools"
$gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleZip = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
$gradleExe = Join-Path $gradleHome "bin\\gradle.bat"
$defaultArgs = @(":sgf-app:integrationTest")

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
Assert-Command -Command "docker" -Message "Docker Desktop no está disponible en PATH."

if (-not $env:DOCKER_HOST) {
    $dockerPing = Invoke-RestMethod -Uri "http://127.0.0.1:2375/_ping" -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($dockerPing -ne "OK") {
        throw "Docker Desktop debe exponer tcp://127.0.0.1:2375 para ejecutar integrationTest en Windows."
    }
    $env:DOCKER_HOST = "tcp://127.0.0.1:2375"
}

docker version | Out-Null

if (-not $GradleArgs -or $GradleArgs.Count -eq 0) {
    $GradleArgs = $defaultArgs
}

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

if (-not (Test-Path $gradleExe)) {
    Write-Host "Descargando Gradle $gradleVersion para tests host..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $gradleZip
    Expand-Archive -Path $gradleZip -DestinationPath $toolsDir -Force
}

$env:GRADLE_USER_HOME = Join-Path $toolsDir "gradle-user-home"
$integrationResultsDir = Join-Path $repoRoot "apps\\api\\sgf-app\\build\\test-results\\integrationTest"

Push-Location (Join-Path $repoRoot "apps\\api")
try {
    if (Test-Path $integrationResultsDir) {
        Remove-Item -Recurse -Force $integrationResultsDir -ErrorAction SilentlyContinue
    }
    Write-Host "Usando DOCKER_HOST=$env:DOCKER_HOST"
    & $gradleExe --no-daemon @GradleArgs
}
finally {
    Pop-Location
}
