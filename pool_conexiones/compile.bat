@echo off
REM ============================================
REM SCRIPT PARA COMPILAR EN WINDOWS
REM ============================================

title Compilando Simulador de Pool de Conexiones

echo ╔════════════════════════════════════════════════════════════════╗
echo ║              COMPILANDO SIMULADOR DE CONEXIONES                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Crear directorio de salida
if not exist "out" mkdir out

REM Detectar drivers en lib
set "PGJAR="
for %%F in (lib\postgresql-*.jar) do if not defined PGJAR set "PGJAR=%%~nxF"
set "MYSQLJAR="
for %%F in (lib\mysql-connector-j-*.jar) do if not defined MYSQLJAR set "MYSQLJAR=%%~nxF"

if not defined PGJAR (
    echo ❌ ERROR: No se encuentra ningun driver PostgreSQL en lib\
    echo.
    echo    DESCARGA NECESARIA:
    echo    1. PostgreSQL JDBC Driver
    echo       URL: https://jdbc.postgresql.org/download/
    echo.
    echo    2. Copiar el JAR a la carpeta lib\
    echo.
    pause
    exit /b 1
)

if not defined MYSQLJAR (
    echo ❌ ERROR: No se encuentra ningun driver MySQL en lib\
    echo.
    echo    DESCARGA NECESARIA:
    echo    1. MySQL JDBC Connector
    echo       URL: https://dev.mysql.com/downloads/connector/j/
    echo.
    echo    2. Copiar el JAR a la carpeta lib\
    echo.
    pause
    exit /b 1
)

for %%F in ("lib\%PGJAR%") do if %%~zF equ 0 (
    echo ❌ ERROR: El archivo %PGJAR% esta vacio o corrupto.
    echo Elimina el archivo y descarga nuevamente el driver PostgreSQL.
    pause
    exit /b 1
)
for %%F in ("lib\%MYSQLJAR%") do if %%~zF equ 0 (
    echo ❌ ERROR: El archivo %MYSQLJAR% esta vacio o corrupto.
    echo Elimina el archivo y descarga nuevamente el driver MySQL.
    pause
    exit /b 1
)

echo ✅ Drivers encontrados: %PGJAR% %MYSQLJAR%
echo.

echo 📦 Compilando archivos Java...
javac -cp "lib\*" ^
      -d out src\main\java\*.java

if %errorlevel% equ 0 (
    echo.
    echo ✅ COMPILACIÓN EXITOSA
    echo.
    echo Los archivos .class se encuentran en la carpeta "out"
    echo.
    echo Para ejecutar el programa, usa: run.bat
) else (
    echo.
    echo ❌ ERROR EN LA COMPILACIÓN
    echo Revisa los mensajes de error arriba
)

echo.
pause