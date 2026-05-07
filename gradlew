#!/usr/bin/env sh
docker run --rm \
  -v "$(pwd):/workspace" \
  -w /workspace/apps/api \
  gradle:8.8.0-jdk21 \
  gradle "$@"

