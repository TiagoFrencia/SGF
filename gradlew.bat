@echo off
setlocal
docker run --rm ^
  -v "%cd%:/workspace" ^
  -w /workspace/apps/api ^
  gradle:8.8.0-jdk21 ^
  gradle %*

