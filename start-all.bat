@echo off
setlocal enabledelayedexpansion

set ROOT_DIR=%~dp0

echo Iniciando backend Spring Boot...
start "wirc-backend" cmd /k "cd /d %ROOT_DIR%backend && mvnw.cmd spring-boot:run"

echo Iniciando frontend na porta 4200...
start "wirc-frontend" cmd /k "cd /d %ROOT_DIR%frontend && python -m http.server 4200"

echo.
echo Backend:  http://localhost:8080/graphql
echo Frontend: http://localhost:4200
