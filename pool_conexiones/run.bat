@echo off
REM ============================================
REM SCRIPT PARA EJECUTAR EN WINDOWS
REM ============================================

title Ejecutando Simulador de Pool de Conexiones

echo ╔════════════════════════════════════════════════════════════════╗
echo ║              EJECUTANDO SIMULADOR DE CONEXIONES                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Verificar que los archivos compilados existen
if not exist "out\main\java\ConnectionSimulatorMultiDB.class" (
    echo ❌ ERROR: No se encuentran los archivos compilados
    echo    Ejecuta primero compile.bat para compilar
    echo.
    pause
    exit /b 1
)

REM Detectar drivers en lib
set "PGJAR="
for %%F in (lib\postgresql-*.jar) do if not defined PGJAR set "PGJAR=%%~nxF"
set "MYSQLJAR="
for %%F in (lib\mysql-connector-j-*.jar) do if not defined MYSQLJAR set "MYSQLJAR=%%~nxF"

if not defined PGJAR (
    echo ❌ ERROR: No se encuentra ningun driver PostgreSQL en lib\
    echo.
    echo    Descarga el driver PostgreSQL en: https://jdbc.postgresql.org/download/
    pause
    exit /b 1
)

if not defined MYSQLJAR (
    echo ❌ ERROR: No se encuentra ningun driver MySQL en lib\
    echo.
    echo    Descarga el driver MySQL en: https://dev.mysql.com/downloads/connector/j/
    pause
    exit /b 1
)

echo ✅ Drivers encontrados: %PGJAR% %MYSQLJAR%
echo.

echo [PASO 2] EJECUTANDO SIMULACION...
echo.

java -cp "out;lib\*" main.java.ConnectionSimulatorMultiDB

echo.
echo ============================================
echo    PROGRAMA FINALIZADO

echo ============================================
echo.
pause