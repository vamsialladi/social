@echo off
echo Starting TrueHunt Spring Boot Backend on port 8080...
cd /d "%~dp0backend"
mvn spring-boot:run
pause
