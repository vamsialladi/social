@echo off
setlocal
echo ========================================================
echo Starting TrueHunt Spring Boot Backend on port 8080...
echo ========================================================

cd /d "%~dp0..\backend"

where mvn >nul 2>nul
if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
) else if exist "C:\Users\vamsi\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\vamsi\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"
) else if exist "C:\Users\vamsi\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=C:\Users\vamsi\tools\apache-maven-3.9.9\bin\mvn.cmd"
) else (
    echo [ERROR] Maven not found. Please ensure Maven is installed and on your PATH.
    pause
    exit /b 1
)

echo Using Maven from: %MVN_CMD%
"%MVN_CMD%" spring-boot:run
pause
