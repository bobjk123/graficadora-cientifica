Set-Location -Path $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       Iniciando Graficadora Científica FX (Liberica JDK)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin\styles")) {
    New-Item -ItemType Directory -Force -Path "bin\styles" | Out-Null
}
Copy-Item "src\main\resources\styles\dark-theme.css" -Destination "bin\styles\dark-theme.css" -Force

Write-Host "Compilando clases Java..." -ForegroundColor Yellow
$sources = Get-ChildItem -Recurse -Path "src\main\java\*.java" | Where-Object { $_.Name -ne "TestRunner.java" } | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d bin $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Ejecutando aplicacion..." -ForegroundColor Green
    Start-Process -FilePath "javaw" -ArgumentList "-cp bin com.graficadora.MainApp"
} else {
    Write-Host "Error durante la compilacion." -ForegroundColor Red
}
