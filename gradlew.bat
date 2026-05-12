@echo off
setlocal
docker run --rm ^
  -u root ^
  -v "%cd%:/workspace" ^
  -v /var/run/docker.sock:/var/run/docker.sock ^
  -w /workspace/apps/api ^
  gradle:8.8.0-jdk21 ^
  gradle %*
