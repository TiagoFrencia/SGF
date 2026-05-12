#!/usr/bin/env sh
set -eu

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
GRADLE_VERSION=8.8
TOOLS_DIR="$REPO_ROOT/.tools"
GRADLE_HOME="$TOOLS_DIR/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
API_DIR="$REPO_ROOT/apps/api"

command -v java >/dev/null 2>&1 || { echo "Java 21+ no está disponible en PATH."; exit 1; }

mkdir -p "$TOOLS_DIR"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Descargando Gradle $GRADLE_VERSION para validación repo-wide..."
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

rm -rf \
  "$REPO_ROOT/apps/api/sgf-app/build/test-results/integrationTest" \
  "$REPO_ROOT/apps/api/sgf-integrations/build/test-results/test" \
  "$REPO_ROOT/apps/api/sgf-ai/build/test-results/test" \
  2>/dev/null || true

run_gradle_task() {
  echo
  echo "==> Ejecutando $1"
  "$GRADLE_BIN" --no-daemon "$1"
}

cd "$API_DIR"
run_gradle_task :sgf-pos:test
run_gradle_task :sgf-integrations:test
run_gradle_task :sgf-ai:test
run_gradle_task :sgf-app:test

echo
echo "==> Ejecutando :sgf-app:integrationTest"
exec "$REPO_ROOT/scripts/test-infra.sh" :sgf-app:integrationTest
