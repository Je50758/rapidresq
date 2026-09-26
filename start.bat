@echo off
title RapidResQ - Local Server
cd /d "%~dp0"

echo ============================================
echo   RapidResQ - Rapid Disaster Response System
echo   Starting local server on port 8080...
echo ============================================
echo.

REM Open the app in the default browser (it will wait until the server is up)
start "" http://localhost:8080/index.html

REM Locate Maven: sibling apache-maven folder, then PATH
set "MVN_CMD="
if exist "%~dp0..\apache-maven-3.9.9\bin\mvn.cmd" set "MVN_CMD=%~dp0..\apache-maven-3.9.9\bin\mvn.cmd"
if not defined MVN_CMD (
    where mvn >nul 2>nul && set "MVN_CMD=mvn"
)
if not defined MVN_CMD (
    echo [ERROR] Maven not found. Install Apache Maven 3.9+ and add it to PATH,
    echo         or place an apache-maven-3.9.9 folder next to this project.
    pause
    exit /b 1
)

REM Run the Spring Boot server in this window (keep this window OPEN while using the app)
call "%MVN_CMD%" -q spring-boot:run

echo.
echo Server stopped.
pause
