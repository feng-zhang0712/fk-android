# Storage (`com.fk.core.storage`)

DataStore façade implementing Pluggable [`KeyValueStore`](../pluggable/storage/KeyValueStore.kt) / [`TypedStore`](../pluggable/storage/KeyValueStore.kt). Phase **A3**.

## Layout

| Type | Role |
|------|------|
| `Storage` | Package marker / version / factory helpers |
| `DataStoreKeyValueStore` | Preferences DataStore `KeyValueStore` (Base64 payloads, key prefix) |
| `EncryptedKeyValueStore` | AES-GCM decorator; key in Android Keystore |
| `StorageKey` / `StringStorageKey` | Namespaced logical keys |
| `StorageException` | Failure model (`InvalidKey`, `DataStoreFailure`, `KeystoreFailure`, …) |

## Usage

```kotlin
// Plain typed preferences
val prefs = Storage.typedStore(context)
prefs.put("demo.note", "hello", String.serializer())
val note = prefs.get("demo.note", String.serializer())

// Encrypted values (Keystore AES-GCM)
val secrets = Storage.encryptedTypedStore(context)
secrets.put("auth.token", "secret", String.serializer())

// Or binary ciphertext without JSON:
val secureKv = Storage.encryptedKeyValueStore(context)
```

Wire into Pluggable:

```kotlin
PluggableServices(storage = Storage.typedStore(context))
```

## Notes

- Sync `KeyValueStore` bridges DataStore via `runBlocking(Dispatchers.IO)` — still prefer
  calling from a background coroutine for bulk work.
- Encrypted and plain stores should use **different** DataStore file names.
- Deferred: TTL / `ExpiringRecord`, file-blob backend, App Group–style multi-process DataStore,
  suspend-first twin of Pluggable `KeyValueStore` (contract is sync today).
