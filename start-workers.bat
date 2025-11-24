@echo off
REM Script para iniciar workers automaticamente

set JAVA_HOME=C:\Program Files\Java\jdk-23
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"

echo Iniciando Worker1 na porta 1099...
start "Worker1" cmd /k java -cp target\classes trabalhofinal.difusaocalor.rmi.WorkerServer Worker1 1099

timeout /t 2 /nobreak

echo Iniciando Worker2 na porta 1100...
start "Worker2" cmd /k java -cp target\classes trabalhofinal.difusaocalor.rmi.WorkerServer Worker2 1100

echo.
echo Workers iniciados! Você pode agora usar o FormPrincipal.
pause
