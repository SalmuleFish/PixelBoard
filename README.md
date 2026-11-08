<div align="center">

# PixelBoard — Security & Privacy Hardened Fork (v1.4.0)

**Versión auditada y endurecida en seguridad y privacidad de PixelBoard (Mod de Gboard v18.5.3 con Rambler Voice Typing, AI Writing Tools y Simple Calculator) [Sin Root].**

<p>
  <img src="https://img.shields.io/badge/Version-v1.4.0--hardened-2E7D32?style=flat-square&logo=shield&logoColor=white" alt="Hardened Release"/>
  <img src="https://img.shields.io/badge/Android-10%20to%2016-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android Support"/>
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-blue?style=flat-square" alt="Arch Support"/>
  <img src="https://img.shields.io/badge/Base%20App-Gboard%20v18.5.3-orange?style=flat-square&logo=google" alt="Base Gboard"/>
  <img src="https://img.shields.io/badge/Root-Not%20Required-success?style=flat-square" alt="No Root"/>
  <img src="https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=flat-square" alt="License"/>
</p>

</div>

---

## 🔒 ¿Qué cambia en este Fork respecto al repositorio original? / Security & Privacy Changes

Un teclado móvil es uno de los componentes más críticos de privacidad en Android. Este fork parte de [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard) (`v1.4.0`) y aplica una **auditoría y remediación completa de seguridad y privacidad** para garantizar que el teclado sea **100% local y conectado únicamente a los servicios oficiales de Google**, sin conexiones a terceros ni claves compartidas:

| Área auditada | En el repositorio original | En este Fork endurecido |
| :--- | :--- | :--- |
| **Auto-actualizador y conexiones a GitHub** | Incluye `PixelBoardUpdateManager`, `PixelBoardUpdateFeature` y `ReleaseInfo` que se conectan a `api.github.com` y descargan APKs. | **Eliminado al 100%** (tanto del código fuente como del binario `.mpp`). Cero conexiones a `api.github.com` ni a ningún servidor ajeno a Google. |
| **Permiso de instalación de paquetes** | Inyecta `android.permission.REQUEST_INSTALL_PACKAGES` en el `AndroidManifest.xml`. | **Eliminado.** El teclado no tiene permiso para solicitar ni instalar paquetes APK. |
| **Exposición de componentes IPC (`AndroidManifest.xml`)** | `GboardPatchesSettingsActivity` se inyecta con `android:exported="true"` y `GboardPatchesSettingsProvider` con `android:grantUriPermissions="true"`. | **Cerrado (`false`).** Ambos componentes quedan con `exported="false"` y `grantUriPermissions="false"` para impedir que otras apps del teléfono lean o modifiquen la configuración del teclado. |
| **Contexto de pantalla (`screen_context_opt_in`)** | Activado por defecto (`true`) al inicializar preferencias. | **Desactivado por defecto (`false`).** Solo se activa si el usuario lo habilita manualmente. |
| **Retención del historial del portapapeles** | Permite la opción `Never` (`876000` horas = 100 años) guardando recortes en texto plano indefinidamente, con `24 horas` por defecto. | **Acotado.** Se eliminó la retención de 100 años (`DURATION_NEVER`). Opciones seguras: `1 hora` (por defecto), `24 horas`, `7 días` y `30 días`. |
| **Verificación de Speech Services de Google** | Comprueba únicamente la existencia del nombre de paquete `com.google.android.tts`. | **Verificación criptográfica SHA-256.** `GboardSpeechServicesDetector` valida la huella del certificado oficial de Google LLC antes de enlazar con el motor de voz. |
| **Calculadora integrada (`Simple Calculator` v1.4.0)** | Lee los últimos 256 caracteres antes del cursor en cualquier campo de texto. | **Protección de campos sensibles.** `GboardCalculatorRuntime` detecta y omite campos de contraseña (`TYPE_TEXT_VARIATION_PASSWORD`, `WEB_PASSWORD`, `NUMBER_VARIATION_PASSWORD`). |
| **Claves criptográficas de firma (Keystores)** | Incluye `morphe.keystore` e `imported.keystore` públicos en el repositorio con contraseñas conocidas. | **Eliminados del repositorio.** Cada usuario genera y utiliza su propia llave privada local RSA-4096 en `tools/patcher-data/` (ignorada por Git). |

---

## 📂 Estructura de Carpetas Clave / Key Directories

```text
PixelBoard/
├── input/                     # 📥 AQUÍ colocas el APK o bundle .apkm original de Gboard (descargado de APKMirror)
├── output/                    # 📤 AQUÍ se genera tu APK final listo para instalar: output/PixelBoard.apk
├── patches/
│   ├── PixelBoard.mpp         # 📦 Paquete de parches precompilado (v1.4.0 endurecido, sin actualizador)
│   └── patches-1.4.0.mpp      # 📦 Copia versionada del paquete de parches
├── tools/
│   ├── patcher.jar            # 🛠️ Herramienta CLI offline para aplicar los parches
│   └── patcher-data/          # 🔑 AQUÍ se guarda tu clave privada local (private-local.jks, ignorada por .gitignore)
├── options.json               # ⚙️ Opciones de parches (nombre de la app, parches habilitados)
└── pixelboard-patches/        # 💻 Código fuente completo (Java + Kotlin) de las extensiones y parches
```

> [!IMPORTANT]
> El archivo [`.gitignore`](.gitignore) está configurado para que **nunca** se suban a Git tus archivos dentro de `input/`, los APKs generados en `output/`, ni tus claves privadas (`.jks`, `.keystore`, `.pass`) en `tools/patcher-data/`.

---

## 🚀 Guía Paso a Paso: Cómo Parchear y Firmar tu Propio APK

### Requisitos Previos
1. **Java 21 o superior** instalado (`java -version`).
2. **Android SDK Build-Tools** (`apksigner` y `keytool`, incluidos con Android Studio o Command Line Tools).

---

### Paso 1: Descargar el APK base oficial de Gboard y colocarlo en `input/`

1. Descarga la versión oficial sin modificar de **Google Gboard `18.5.3.993608717-release-arm64-v8a`** desde [APKMirror](https://www.apkmirror.com/apk/google-inc/gboard/gboard-the-google-keyboard-18-5-3-993608717-release/) (puede ser el archivo `.apk` o el paquete `.apkm`).
2. Coloca el archivo descargado dentro de la carpeta **`input/`**:
   - Ejemplo: `input/gboard.apkm` (o `input/gboard.apk`).

---

### Paso 2: Generar tu Clave Privada Personal (Solo la primera vez)

Para que nadie más pueda suplantar actualizaciones de tu teclado en tu teléfono, genera tu propia llave privada RSA-4096 dentro de **`tools/patcher-data/private-local.jks`**:

#### En Windows (PowerShell):
```powershell
New-Item -ItemType Directory -Force -Path "tools\patcher-data" | Out-Null

# Cambia "TU_CONTRASENA_SEGURA" por la contraseña que prefieras y guárdala en private-local.pass
$ksPass = "TU_CONTRASENA_SEGURA"
Set-Content -Path "tools\patcher-data\private-local.pass" -Value $ksPass -NoNewline

keytool -genkeypair `
  -keystore "tools\patcher-data\private-local.jks" `
  -storetype PKCS12 `
  -alias pixelboard_local `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -storepass $ksPass `
  -keypass $ksPass `
  -dname "CN=PixelBoard Local Private Key, OU=Security Hardened, O=Private, L=Local, ST=Local, C=US"
```

#### En Linux / macOS (Bash):
```bash
mkdir -p tools/patcher-data
KS_PASS="TU_CONTRASENA_SEGURA"
printf "%s" "$KS_PASS" > tools/patcher-data/private-local.pass

keytool -genkeypair \
  -keystore tools/patcher-data/private-local.jks \
  -storetype PKCS12 \
  -alias pixelboard_local \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$KS_PASS" \
  -keypass "$KS_PASS" \
  -dname "CN=PixelBoard Local Private Key, OU=Security Hardened, O=Private, L=Local, ST=Local, C=US"
```

> [!WARNING]
> **Conserva siempre tu archivo `tools/patcher-data/private-local.jks` y `tools/patcher-data/private-local.pass`.**
> Cuando en el futuro compiles una versión nueva de PixelBoard y la firmes con este mismo archivo `.jks`, Android te permitirá **actualizar la app directamente encima** sin desinstalar ni perder tu diccionario o preferencias.

---

### Paso 3: Aplicar los Parches y Generar el APK en `output/PixelBoard.apk`

Ejecuta el parcheador usando el paquete precompilado `patches/PixelBoard.mpp` y firma el resultado con tu llave privada:

#### En Windows (PowerShell):
```powershell
$inputFile = (Get-ChildItem "input\*" -Include *.apk, *.apkm | Select-Object -First 1).FullName
$ksPass = (Get-Content "tools\patcher-data\private-local.pass" -Raw).Trim()
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" # Ajusta la versión de build-tools si es distinta

# 1. Aplicar los 5 parches sobre el APK/APKM de entrada
java -jar "tools\patcher.jar" patch `
  --patches="patches\PixelBoard.mpp" `
  --out="output\PixelBoard-unsigned.apk" `
  --temporary-files-path="tools\patcher-data\tmp" `
  --unsigned `
  -e "AI Writing Tools" `
  -e "Package Rename" -Oapp_display_name="PixelBoard" `
  -e "Advanced Voice Typing" `
  -e "Simple Calculator" `
  -e "Add Gboard Signature Bypass" `
  "$inputFile"

# 2. Firmar el APK con tu llave privada local
& $apksigner sign `
  --ks "tools\patcher-data\private-local.jks" `
  --ks-key-alias "pixelboard_local" `
  --ks-pass "pass:$ksPass" `
  --key-pass "pass:$ksPass" `
  --out "output\PixelBoard.apk" `
  "output\PixelBoard-unsigned.apk"

Remove-Item -Force "output\PixelBoard-unsigned.apk", "output\*.idsig" -ErrorAction SilentlyContinue

# 3. Verificar la firma del APK generado
& $apksigner verify --verbose --print-certs "output\PixelBoard.apk"
```

#### En Linux / macOS (Bash):
```bash
INPUT_FILE=$(ls input/*.apk input/*.apkm 2>/dev/null | head -n 1)
KS_PASS=$(cat tools/patcher-data/private-local.pass)

# 1. Aplicar los parches
java -jar tools/patcher.jar patch \
  --patches="patches/PixelBoard.mpp" \
  --out="output/PixelBoard-unsigned.apk" \
  --temporary-files-path="tools/patcher-data/tmp" \
  --unsigned \
  -e "AI Writing Tools" \
  -e "Package Rename" -Oapp_display_name="PixelBoard" \
  -e "Advanced Voice Typing" \
  -e "Simple Calculator" \
  -e "Add Gboard Signature Bypass" \
  "$INPUT_FILE"

# 2. Firmar con apksigner
apksigner sign \
  --ks "tools/patcher-data/private-local.jks" \
  --ks-key-alias "pixelboard_local" \
  --ks-pass "pass:$KS_PASS" \
  --key-pass "pass:$KS_PASS" \
  --out "output/PixelBoard.apk" \
  "output/PixelBoard-unsigned.apk"

rm -f output/PixelBoard-unsigned.apk output/*.idsig
apksigner verify --verbose --print-certs output/PixelBoard.apk
```

### ¿Dónde sale el resultado final?
Al terminar, tu APK firmado y listo para instalar en el teléfono se encuentra en:
- **`output/PixelBoard.apk`**

---

## ✨ Funciones Incluidas (v1.4.0)

1. **Gemini "Rambler" Natural Voice Typing (`Advanced Voice Typing`)**:
   - Dictado fluido con puntuación automática, limpieza de muletillas y botón de minimizar barra de voz restaurado.
2. **Asistente de Escritura con IA (`AI Writing Tools`)**:
   - Corrección ortográfica/gramatical en un toque (*Proofread / Fix it*) y reescritura de tonos en cualquier aplicación.
3. **Calculadora Simple en Línea (`Simple Calculator` — Nuevo en v1.4.0)**:
   - Calcula expresiones matemáticas (`12+34`, `(5*8)/2`, `2+3×4`) directamente en la barra de sugerencias mientras escribes; toca el resultado para reemplazar la operación o mantenlo presionado para copiarlo. Se activa en **Ajustes de PixelBoard → Advanced settings → Activar Calculadora simple**.
4. **Portapapeles Mejorado (`PR #129`)**:
   - Límite ampliado de 5 a 100 recortes recientes, sincronización en primer plano para Samsung One UI / Vivo y selector de retención seguro (`1 hora`, `24 horas`, `7 días`, `30 días`).
5. **Personalización Material 3 (`v1.3.3`)**:
   - Teclas redondeadas (Belka UI), fuente **Google Sans Flex**, controles de **Deshacer/Rehacer (Undo)** en la barra de sugerencias y barra rápida de emojis frecuentes / Magic Emoji.
6. **Instalación en Paralelo (`Package Rename` + `Signature Bypass`)**:
   - Se instala bajo el paquete `com.akshaykadam.pixelboard` para convivir sin conflictos junto al Gboard de fábrica sin requerir Root.

---

## 📜 Créditos y Licencia

- **Proyecto original PixelBoard:** [Akshay Kadam (@Akshayykadam)](https://github.com/Akshayykadam/PixelBoard)
- **Framework base de parches de Gboard:** JasonWu y las comunidades de código abierto de ReVanced / Morphe.
- Distribuido bajo la licencia **GNU General Public License v3.0 (GPLv3)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
