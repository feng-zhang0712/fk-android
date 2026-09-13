# Mapping (`com.fk.core.mapping`)

kotlinx.serialization conventions and business envelope helpers. Phase **A5**.

## Layout

| Type | Role |
|------|------|
| `Mapping` / `MappingJson` | Package marker + shared `Json` presets (`Api`, `LenientApi`, `Strict`) |
| `JsonMapper` | Encode/decode + envelope unwrap |
| `EnvelopeConfig` / `EnvelopeProcessor` | `{ code, message, data }` (and success-flag) envelopes |
| `EnvelopeResponseInterceptor` | Pluggable `ResponseInterceptor` for 2xx unwrap |
| `Page` / `ListResponse` | Pagination templates |
| `MappingException` | Invalid JSON, decode/encode, key missing, business failure |

## Usage

```kotlin
@Serializable
data class UserDto(val userId: String, val displayName: String)

val mapper = Mapping.apiMapper()

// snake_case JSON ↔ camelCase properties
val user = mapper.decode(
  """{"user_id":"u1","display_name":"Ada"}""".toByteArray(),
  UserDto.serializer(),
)

// Envelope
val profile = mapper.decodeEnvelope(
  """{"code":0,"message":"ok","data":{"user_id":"u1","display_name":"Ada"}}""".toByteArray(),
  UserDto.serializer(),
)
```

## Notes

- Storage continues to use `PluggableJson` — do not conflate with `MappingJson`.
- `EnvelopeResponseInterceptor` implements Pluggable `ResponseInterceptor`; wire it in
  the host (or once `:core` network applies a response-interceptor chain).
- Deferred: dictionary/`FKMappable` path mapping, polymorphic registries, custom date transforms.
