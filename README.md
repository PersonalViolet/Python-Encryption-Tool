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

- Python 3.8+
- Dependencies listed in `requirements.txt`:
  - `cryptography`
  - `tkinterdnd2` (optional, for drag-and-drop support)

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

## Testing

A sample test script `verify_and_generate_samples.py` is included to verify the cryptographic logic without the GUI.
A sample file `test_sample.txt` and its encrypted version `test_sample.txt.enc` are provided.
The password for the sample is: `TestSig123`
