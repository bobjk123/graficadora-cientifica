@echo off
title Graficadora Cientifica FX
cd /d "%~dp0"

echo ========================================================
echo        Iniciando Graficadora Cientifica FX
echo ========================================================

if not exist "bin" mkdir bin
if not exist "bin\styles" mkdir bin\styles
copy /y "src\main\resources\styles\dark-theme.css" "bin\styles\dark-theme.css" >nul 2>&1

echo Compilando codigo fuente con UTF-8...
javac -encoding UTF-8 -d bin src\main\java\com\graficadora\model\*.java src\main\java\com\graficadora\ui\*.java src\main\java\com\graficadora\MainApp.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Ocurrio un error al compilar el proyecto.
    pause
    exit /b %ERRORLEVEL%
)

echo Lanzando aplicacion JavaFX...
start javaw -cp bin com.graficadora.MainApp
exit /b 0
