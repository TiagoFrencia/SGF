#!/usr/bin/env sh
set -eu

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
GRADLE_VERSION=8.8
TOOLS_DIR="$REPO_ROOT/.tools"
GRADLE_HOME="$TOOLS_DIR/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ "$#" -eq 0 ]; then
  set -- :sgf-app:integrationTest
fi

command -v java >/dev/null 2>&1 || { echo "Java 21+ no está disponible en PATH."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Docker no está disponible en PATH."; exit 1; }

if [ -z "${DOCKER_HOST:-}" ]; then
  if command -v curl >/dev/null 2>&1 && curl -fsS http://127.0.0.1:2375/_ping >/dev/null 2>&1; then
    export DOCKER_HOST="tcp://127.0.0.1:2375"
  else
    echo "Docker debe responder en tcp://127.0.0.1:2375 para ejecutar integrationTest en Windows."
    exit 1
  fi
fi

docker version >/dev/null

mkdir -p "$TOOLS_DIR"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Descargando Gradle $GRADLE_VERSION para tests host..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$GRADLE_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$GRADLE_ZIP"
  else
    echo "Se requiere curl o wget para descargar Gradle."
    exit 1
  fi
  unzip -oq "$GRADLE_ZIP" -d "$TOOLS_DIR"
fi

export GRADLE_USER_HOME="$TOOLS_DIR/gradle-user-home"
INTEGRATION_RESULTS_DIR="$REPO_ROOT/apps/api/sgf-app/build/test-results/integrationTest"
rm -rf "$INTEGRATION_RESULTS_DIR" 2>/dev/null || true
cd "$REPO_ROOT/apps/api"
echo "Usando DOCKER_HOST=$DOCKER_HOST"
exec "$GRADLE_BIN" --no-daemon "$@"
