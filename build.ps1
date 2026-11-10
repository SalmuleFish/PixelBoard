# ==============================================================================
# PixelBoard - Build & Patch Automation Script (Windows PowerShell)
# ==============================================================================

param(
    [string]$InputApk = "",
    [switch]$Help
)

if ($Help) {
    Write-Host "Uso: .\build.ps1 [-InputApk <ruta_apk>]"
    Write-Host "Por defecto, busca cualquier archivo .apk o .apkm dentro de input/ y genera output/PixelBoard.apk"
    exit 0
}

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# 1. Resolver APK de entrada
if (-not $InputApk) {
    $candidates = Get-ChildItem -Path "input" -File | Where-Object { $_.Extension -in @(".apk", ".apkm") }
    if ($candidates) {
        $InputApk = $candidates[0].FullName
    } else {
        Write-Host "⚠️ No se encontró ningún APK en input/. Coloca el APK o .apkm oficial de Gboard en la carpeta input/." -ForegroundColor Yellow
        exit 0
    }
}

if (-not (Test-Path $InputApk)) {
    Write-Host "❌ Error: El archivo de entrada '$InputApk' no existe." -ForegroundColor Red
    exit 1
}

Write-Host "📥 APK base detectado: $InputApk" -ForegroundColor Cyan

# 2. Verificar Java 21+
$javaCmd = Get-Command "java" -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "❌ Error: Java no está instalado en el PATH." -ForegroundColor Red
    exit 1
}

# 3. Directorios y Rutas
$OutputFolder = Join-Path $ScriptDir "output"
if (-not (Test-Path $OutputFolder)) { New-Item -ItemType Directory -Path $OutputFolder | Out-Null }

$ToolsFolder = Join-Path $ScriptDir "tools"
$PatcherJar = Join-Path $ToolsFolder "patcher.jar"
$MppPath = Join-Path $ScriptDir "patches\PixelBoard.mpp"
$KeystorePath = Join-Path $ToolsFolder "patcher-data\private-local.jks"
$PassFile = Join-Path $ToolsFolder "patcher-data\private-local.pass"
$KeystoreAlias = "pixelboard_local"

if (-not (Test-Path $MppPath)) {
    Write-Host "❌ Error: No se encontró el paquete de parches en $MppPath" -ForegroundColor Red
    exit 1
}

# 4. Generar Keystore local si no existe
if (-not (Test-Path $KeystorePath)) {
    Write-Host "🔑 Generando nueva clave privada local RSA-4096..." -ForegroundColor Yellow
    $pass = [System.Guid]::NewGuid().ToString("N")
    Set-Content -Path $PassFile -Value $pass
    $keytoolArgs = @(
        "-genkeypair",
        "-alias", $KeystoreAlias,
        "-keyalg", "RSA",
        "-keysize", "4096",
        "-validity", "10000",
        "-keystore", $KeystorePath,
        "-storepass", $pass,
        "-keypass", $pass,
        "-dname", "CN=PixelBoard Local Private Key, OU=Security Hardened, O=Private, L=Local, ST=Local, C=US"
    )
    & keytool $keytoolArgs
    Write-Host "✅ Clave generada exitosamente en $KeystorePath" -ForegroundColor Green
}

$KeystorePass = (Get-Content $PassFile -Raw).Trim()

# 5. Aplicar parches con patcher.jar
$TempFolder = Join-Path $ScriptDir "input\tmp_patching"
if (Test-Path $TempFolder) { Remove-Item -Recurse -Force $TempFolder }
New-Item -ItemType Directory -Path $TempFolder | Out-Null

$UnsignedApk = Join-Path $TempFolder "PixelBoard-unsigned.apk"
$FinalApk = Join-Path $OutputFolder "PixelBoard.apk"

Write-Host "=== 🔨 Aplicando parches offline con Morphe Patcher ===" -ForegroundColor Cyan
$patchArgs = @(
    "-jar", $PatcherJar,
    "patch",
    "-p", $MppPath,
    "-o", $UnsignedApk,
    "-t", $TempFolder,
    "--options-file", "options.json",
    "--striplibs=arm64-v8a",
    "--unsigned",
    "-f",
    $InputApk
)
& java $patchArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error durante la aplicación de parches." -ForegroundColor Red
    exit 1
}

# 6. Alinear y firmar con la clave privada local
Write-Host "=== 🛡️ Alineando y firmando con clave privada local ===" -ForegroundColor Cyan
& python "$ToolsFolder\sanitize_apk_resources.py" $UnsignedApk $InputApk $KeystorePath $KeystorePass $KeystoreAlias

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error durante la firma del APK." -ForegroundColor Red
    exit 1
}

# Limpieza y resultado
Copy-Item $UnsignedApk $FinalApk -Force
Remove-Item -Recurse -Force $TempFolder
Get-ChildItem -Path "input" -Directory -Filter "tmp_*" | Remove-Item -Recurse -Force

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "🎉 ¡APK generado y firmado con éxito!" -ForegroundColor Green
Write-Host "📱 Resultado: $FinalApk" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

