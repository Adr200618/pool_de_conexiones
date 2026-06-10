@echo off
title COMPILANDO Y EJECUTANDO SIMULADOR

echo ================================================
echo    SIMULADOR DE POOL DE CONEXIONES
echo    PostgreSQL vs MySQL
echo ================================================
echo.

REM ========== COMPILAR ==========
echo [PASO 1] COMPILANDO...
echo.

REM Crear directorio de salida
if not exist "out" mkdir out

REM Detectar drivers en lib
set "PGJAR="
for %%F in (lib\postgresql-*.jar) do if not defined PGJAR set "PGJAR=%%~nxF"
set "MYSQLJAR="
for %%F in (lib\mysql-connector-j-*.jar) do if not defined MYSQLJAR set "MYSQLJAR=%%~nxF"

if not defined PGJAR (
    echo [ERROR] No se encuentra ningun driver PostgreSQL en lib\
    echo.
    echo SOLUCION:
    echo 1. Descargar de: https://jdbc.postgresql.org/download/
    echo 2. Guardar el JAR en la carpeta "lib"
    pause
    exit /b 1
)

if not defined MYSQLJAR (
    echo [ERROR] No se encuentra ningun driver MySQL en lib\
    echo.
    echo SOLUCION:
    echo 1. Descargar de: https://dev.mysql.com/downloads/connector/j/
    echo 2. Guardar el JAR en la carpeta "lib"
    pause
    exit /b 1
)

for %%F in ("lib\%PGJAR%") do if %%~zF equ 0 (
    echo [ERROR] El archivo %PGJAR% esta vacio o corrupto.
    echo Elimina el archivo y descarga nuevamente el driver PostgreSQL.
    pause
    exit /b 1
)
for %%F in ("lib\%MYSQLJAR%") do if %%~zF equ 0 (
    echo [ERROR] El archivo %MYSQLJAR% esta vacio o corrupto.
    echo Elimina el archivo y descarga nuevamente el driver MySQL.
    pause
    exit /b 1
)

echo [OK] Drivers encontrados: %PGJAR% %MYSQLJAR%
echo.

echo Compilando...
javac -cp "lib\*" -d out src\main\java\*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Fallo la compilacion
    echo Revisa los errores arriba
    pause
    exit /b 1
)

echo [OK] Compilacion exitosa
echo.

REM ========== EJECUTAR ==========
echo [PASO 2] EJECUTANDO SIMULACION...
echo.

java -cp "out;lib\*" main.java.ConnectionSimulatorMultiDB

echo.
echo ================================================
echo    PROGRAMA FINALIZADO
echo ================================================
echo.
pause