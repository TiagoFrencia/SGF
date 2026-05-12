#!/usr/bin/env sh
docker run --rm \
  -u root \
  -v "$(pwd):/workspace" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w /workspace/apps/api \
  gradle:8.8.0-jdk21 \
  gradle "$@"

