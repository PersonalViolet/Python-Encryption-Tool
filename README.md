# Python Encryption Tool (PBKDF2 + AES)

This is a GUI-based encryption tool built with Python and Tkinter. It uses AES (CBC mode) for encryption and PBKDF2 for key derivation, ensuring high security for both text and files.

## Features

- **Language Support**: Switch between English and Chinese (中文) instantly.
- **Text Encryption/Decryption**: Securely encrypt messages with a custom signature (password).
- **File Encryption/Decryption**: Support for large files with progress tracking.
- **Drag & Drop**: Easily drag files into the application (requires `tkinterdnd2`).
- **Security**: 
  - AES-256 (default), AES-192, AES-128 support.
  - PBKDF2-HMAC-SHA256 with 10,000 iterations for key derivation.
  - Random Salt and IV generated for every operation.
  - PKCS7 Padding.

## Requirements

- Python 3.10+
- Dependencies listed in `requirements.txt`

## Installation

1. Clone or download this repository.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
   *Note: If `tkinterdnd2` fails to install, the application will still work but without drag-and-drop functionality.*

## Usage

Run the application:
```bash
python main.py
```

### 1. Control Panel (Top)
- **Signature**: Enter your password/passphrase here. This is used to derive the encryption key.
- **Confirm Sign**: Optional visual confirmation to lock in your intent.
- **Algorithm**: Choose between AES-128, AES-192, or AES-256 (default).
- **Language**: Toggle between English and Chinese interface.
- **Set Output Dir**: Choose where encrypted/decrypted files should be saved by default.

### 2. Text Encryption (Left)
- Enter text in the bottom box.
- Click **Encrypt**.
- The result (Base64 encoded) appears in the top history log.

### 3. Text Decryption (Middle)
- Paste the Base64 encrypted string into the bottom box.
- Ensure the **Signature** matches the one used for encryption.
- Click **Decrypt**.
- The original text appears in the top history log.

### 4. File Operations (Right)
- **Encrypt File**:
  - Drag a file into the top box OR click "Select File".
  - Click **Encrypt File**.
  - The file will be saved with a `.enc` extension in the selected output directory (or same folder).
- **Decrypt File**:
  - Drag an encrypted file (`.enc`) into the bottom box.
  - Click **Decrypt File**.
  - The file will be decrypted with a `.dec` extension.

## Technical Details

- **Key Derivation**: 
  - Function: `PBKDF2HMAC(SHA256)`
  - Iterations: 10,000
  - Salt: 16 bytes (Randomly generated per encryption, stored in output)
- **Encryption**:
  - Algorithm: AES (CBC Mode)
  - IV: 16 bytes (Randomly generated per encryption, stored in output)
  - Padding: PKCS7 (128-bit block size)
- **File Format**:
  - `[Salt (16 bytes)] [IV (16 bytes)] [Encrypted Data ...]`

## Android App

An Android mobile version is available in the `android/` directory.

### Features
- Same AES-CBC + PBKDF2 crypto as the desktop version — cross-platform compatible
- Jetpack Compose UI with Material 3 design
- Responsive layout: three-column on tablets, tab-based on phones
- Chinese/English language switching
- Settings persistence via DataStore

### Build & Run

**Option 1: Android Studio** (recommended)
1. Open the `android/` directory in Android Studio
2. Wait for Gradle sync to complete
3. Click **Run** to deploy to a connected device or emulator

**Option 2: Command Line**
```bash
# First, generate the Gradle wrapper (requires Gradle installed):
cd android
gradle wrapper

# Or open the project in Android Studio which auto-generates the wrapper.
# Then build:
cd android
./gradlew assembleDebug
# APK output: android/app/build/outputs/apk/debug/app-debug.apk
```

**Run unit tests:**
```bash
cd android
./gradlew testDebugUnitTest
```

### CI/CD
GitHub Actions (`.github/workflows/android-build.yml`) automatically builds and tests the Android app on every push. The debug APK is uploaded as an artifact.

# ⚠️ 免责声明

本仓库包含的加密/解密程序仅为**学习、研究和实验目的**而提供。作者不保证其安全性、完整性或适用性，使用本程序所产生的一切风险及后果由使用者自行承担。

## 重要声明

1. **非专业用途**
   本程序仅为教学示例，**不适用于**保护真实场景中的敏感数据。其加密实现可能包含漏洞，无法达到工业级安全标准。
2. **禁止用于非法用途**
   使用者不得将本程序用于任何非法活动，包括但不限于侵犯隐私、破坏系统或违反当地法律法规的行为。作者对任何滥用行为概不负责。
3. **无担保责任**
   本程序按“原样”提供，作者明确不承担任何明示或暗示的担保责任，包括但不限于对适用性、特定功能或安全性的担保。使用本程序造成的任何直接或间接损失，作者不承担法律责任。
4. **自行承担风险**
   任何使用者应自行评估程序的安全性及合规性，并在必要时咨询安全专家或法律顾问。

## 建议

- 请勿使用本程序处理真实敏感数据。
- 欢迎提交问题或改进代码，但请勿要求作者提供安全审计或技术支持。
- 如发现安全漏洞，请通过 GitHub Issues 告知。
