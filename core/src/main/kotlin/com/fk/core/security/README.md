# Security (`com.fk.core.security`)

Hash / AES / RSA / HMAC / masking / random + Keystore-backed key storage. Phase **B1**.

## Layout

| Type | Role |
|------|------|
| `Security` / `FkSecurity` | Package marker + service bag / factories |
| `Hasher` | MD5 / SHA-1 / SHA-256 / SHA-512 (HEX) |
| `AesCryptor` | AES CBC/ECB + PKCS5Padding; Base64 string helpers |
| `RsaCryptor` | Keygen, OAEP/PKCS1 encrypt, SHA-256/512 sign |
| `HmacSigner` | HMAC + sorted parameter signing |
| `SecurityCodec` | Base64 / HEX / URL |
| `SecurityUtils` | SecureRandom, phone/ID/email mask, wipe |
| `SecretKeyStore` / `AndroidSecretKeyStore` | Persist raw keys wrapped by Android Keystore AES-GCM |
| `SecurityException` | Invalid input/key, crypto, keystore, not found |

## Usage

```kotlin
val security = Security.create(context)
val key = security.aes.generateKey()
val iv = security.aes.generateIv()
val cipher = security.aes.encryptToBase64("hello", key, iv)
val plain = security.aes.decryptFromBase64(cipher, key, iv)

security.secretKeyStore?.put("aes.session", key)
val restored = security.secretKeyStore?.get("aes.session")
```

## Notes

- Prefer SHA-256+ and AES-CBC (or app-level GCM via `:core` storage) over MD5/SHA-1/ECB.
- Anti-debug / executable snapshot helpers from iOS are deferred (platform-specific).
- File streaming AES is deferred; use in-memory APIs for typical request payloads.
