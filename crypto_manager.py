import os
import base64
import logging
import time
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.backends import default_backend

# Setup basic logging to file just in case, though GUI will handle display
logging.basicConfig(filename='crypto_ops.log', level=logging.INFO, 
                    format='%(asctime)s - %(levelname)s - %(message)s')

class CryptoManager:
    SALT_SIZE = 16
    IV_SIZE = 16
    ITERATIONS = 10000
    CHUNK_SIZE = 64 * 1024  # 64KB

    ALGORITHMS = {
        "AES-128": 16,  # 128 bits = 16 bytes
        "AES-192": 24,  # 192 bits = 24 bytes
        "AES-256": 32   # 256 bits = 32 bytes
    }

    def __init__(self):
        pass

    def _derive_key(self, password: str, salt: bytes, key_length: int) -> bytes:
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=key_length,
            salt=salt,
            iterations=self.ITERATIONS,
            backend=default_backend()
        )
        return kdf.derive(password.encode('utf-8'))

    # AES Padding mode, focus on encrypt_text
    def _pad_data(self, data: bytes) -> bytes:
        padder = padding.PKCS7(128).padder()
        padded_data = padder.update(data) + padder.finalize()
        return padded_data

    def _unpad_data(self, data: bytes) -> bytes:
        unpadder = padding.PKCS7(128).unpadder()
        return unpadder.update(data) + unpadder.finalize()

    def encrypt_text(self, text: str, signature: str, algo_name: str) -> str:
        try:
            if not text:
                return ""
            
            key_len = self.ALGORITHMS.get(algo_name, 32)
            salt = os.urandom(self.SALT_SIZE)
            iv = os.urandom(self.IV_SIZE)

            # Derive key using PBKDF2, salt and key length
            key = self._derive_key(signature, salt, key_len)

            # AES encryption
            # Create encryptor
            cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
            encryptor = cipher.encryptor()
            #use AES CBC mode
            data_bytes = text.encode('utf-8')
            padded_data = self._pad_data(data_bytes)
            ciphertext = encryptor.update(padded_data) + encryptor.finalize()
            # Combine: Salt + IV + Ciphertext
            combined = salt + iv + ciphertext
            return base64.b64encode(combined).decode('utf-8')
        except Exception as e:
            logging.error(f"Text encryption failed: {str(e)}")
            raise e

    def decrypt_text(self, encrypted_b64: str, signature: str, algo_name: str) -> str:
        try:
            if not encrypted_b64:
                return ""
                
            combined = base64.b64decode(encrypted_b64)
            
            if len(combined) < self.SALT_SIZE + self.IV_SIZE:
                raise ValueError("Invalid encrypted data format")

            # Extract Salt + IV + Ciphertext
            salt = combined[:self.SALT_SIZE]
            iv = combined[self.SALT_SIZE:self.SALT_SIZE + self.IV_SIZE]
            ciphertext = combined[self.SALT_SIZE + self.IV_SIZE:]
            
            key_len = self.ALGORITHMS.get(algo_name, 32)
            key = self._derive_key(signature, salt, key_len)

            # Create decryptor
            cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
            decryptor = cipher.decryptor()
            
            padded_data = decryptor.update(ciphertext) + decryptor.finalize()
            original_data = self._unpad_data(padded_data)
            
            return original_data.decode('utf-8')
        except Exception as e:
            logging.error(f"Text decryption failed: {str(e)}")
            raise e

    def encrypt_file(self, input_path: str, output_path: str, signature: str, algo_name: str, progress_callback=None):
        try:
            key_len = self.ALGORITHMS.get(algo_name, 32)
            salt = os.urandom(self.SALT_SIZE)
            iv = os.urandom(self.IV_SIZE)
            
            key = self._derive_key(signature, salt, key_len)
            
            cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
            encryptor = cipher.encryptor()
            
            file_size = os.path.getsize(input_path)
            processed_size = 0
            
            # with open(input_path, 'rb') as f_in, open(output_path, 'wb') as f_out:
            #     # Write header: Salt + IV
            #     f_out.write(salt)
            #     f_out.write(iv)
            #
            #     while True:
            #         chunk = f_in.read(self.CHUNK_SIZE)
            #         if len(chunk) == 0:
            #             break
            #
            #         processed_size += len(chunk)
            #
            #         if len(chunk) < self.CHUNK_SIZE:
            #             # Last chunk, needs padding
            #             padded_chunk = self._pad_data(chunk)
            #             f_out.write(encryptor.update(padded_chunk) + encryptor.finalize())
            #         else:
            #             # Full chunk.
            #             # Note: Padding PKCS7 on stream is tricky.
            #             # Usually we treat the whole file as one message.
            #             # Standard approach for streams: Pad only the last block.
            #             # Cryptography's padder handles this if we feed it right,
            #             # but we need to know if it's the last chunk.
            #             # The 'padder' object is stateful? No, padder is for one-shot or update().
            #             # Actually, `padding.PKCS7` padder doesn't support stream updating easily if we don't know the end.
            #             # BUT, we do know the end (len(chunk) < CHUNK_SIZE or next read is empty).
            #             # Let's use a simpler approach: Read all? No, memory issues.
            #             # Correct streaming padding:
            #             # 1. Read chunk.
            #             # 2. If it's the last chunk (EOF), pad it and encrypt.
            #             # 3. If not last, encrypt raw.
            #             # Wait, AES requires blocks of 16 bytes. If chunk is 64KB, it's a multiple of 16.
            #             # So we can encrypt full chunks directly.
            #             # Only the last chunk needs padding.
            #
            #             # Logic revision:
            #             # AES CBC works on blocks. 64KB is multiple of 16.
            #             # We can encrypt valid blocks directly.
            #             # We only need the padder for the *final* bytes.
            #             # BUT, PKCS7 always adds padding, even if multiple of 16 (adds a full block of 16s).
            #             # So we can effectively say:
            #             # - Loop read.
            #             # - If next read is empty, this was the last chunk.
            #             # - But we don't know if next read is empty until we try.
            #             # Buffered approach:
            #             pass
            #
            #         if progress_callback:
            #             progress_callback(processed_size, file_size)

            # Re-implementing file loop for correct padding
            # We need to ensure we pad the final block.
            # And we must ensure previous blocks are multiples of 16.
            
            # Reset
            encryptor = cipher.encryptor() # Fresh encryptor
            padder = padding.PKCS7(128).padder()
            
            with open(input_path, 'rb') as f_in, open(output_path, 'wb') as f_out:
                f_out.write(salt)
                f_out.write(iv)
                
                while True:
                    chunk = f_in.read(self.CHUNK_SIZE)
                    if len(chunk) == 0:
                        # End of file. Finalize padding.
                        # The padder might have pending data? No, we feed chunk by chunk.
                        # If we feed chunk to padder.update(), it buffers.
                        # Let's do this:
                        final_data = padder.finalize()
                        f_out.write(encryptor.update(final_data) + encryptor.finalize())
                        break
                    
                    # Update padder with chunk
                    padded_chunk_part = padder.update(chunk)
                    # Encrypt whatever the padder spits out (it keeps internal buffer for incomplete blocks)
                    encrypted_part = encryptor.update(padded_chunk_part)
                    f_out.write(encrypted_part)
                    
                    processed_size += len(chunk)
                    if progress_callback:
                        progress_callback(processed_size, file_size)

        except Exception as e:
            logging.error(f"File encryption failed: {str(e)}")
            raise e

    def decrypt_file(self, input_path: str, output_path: str, signature: str, algo_name: str, progress_callback=None):
        try:
            file_size = os.path.getsize(input_path)
            # Header size
            header_size = self.SALT_SIZE + self.IV_SIZE
            if file_size < header_size:
                raise ValueError("File too small to be a valid encrypted file")
            
            with open(input_path, 'rb') as f_in:
                salt = f_in.read(self.SALT_SIZE)
                iv = f_in.read(self.IV_SIZE)
                
                key_len = self.ALGORITHMS.get(algo_name, 32)
                key = self._derive_key(signature, salt, key_len)
                
                cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
                decryptor = cipher.decryptor()
                unpadder = padding.PKCS7(128).unpadder()
                
                processed_size = header_size
                
                with open(output_path, 'wb') as f_out:
                    while True:
                        chunk = f_in.read(self.CHUNK_SIZE)
                        if len(chunk) == 0:
                            # End of stream
                            final_unpadded = unpadder.finalize()
                            f_out.write(final_unpadded)
                            # Decryptor finalize usually returns empty for CBC if aligned? 
                            # Actually decryptor.finalize() checks for leftover bytes which shouldn't exist in CBC if padded correctly.
                            decryptor.finalize() 
                            break
                        
                        decrypted_chunk = decryptor.update(chunk)
                        unpadded_chunk_part = unpadder.update(decrypted_chunk)
                        f_out.write(unpadded_chunk_part)
                        
                        processed_size += len(chunk)
                        if progress_callback:
                            progress_callback(processed_size, file_size)
                            
        except Exception as e:
            logging.error(f"File decryption failed: {str(e)}")
            raise e
