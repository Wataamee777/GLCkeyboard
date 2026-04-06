@echo off
where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

echo gradle command not found. Please install Gradle or add a Gradle wrapper jar. 1>&2
exit /b 127
