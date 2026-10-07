@echo off
setlocal enabledelayedexpansion

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\jdk-21"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)

where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set "MAVEN_HOME=%LOCALAPPDATA%\Programs\apache-maven-3.9.9"
    set "PATH=!MAVEN_HOME!\bin;!PATH!"
)

cd /d "%~dp0"

echo ==========================================
echo    Avvio DriveHub (JavaFX + Maven)
echo ==========================================
echo.

call mvn javafx:run
