# Network (`com.fk.core.network`)

OkHttp façade implementing Pluggable [`ApiClient`](../pluggable/networking/ApiClient.kt). Phase **A2**.

## Layout

| Type | Role |
|------|------|
| `Network` | Package marker / version |
| `NetworkConfig` | Timeouts, base URL, default headers, optional HTTP logging |
| `OkHttpApiClient` | `ApiClient` implementation |
| `NetworkException` | Transport failure model (`InvalidUrl`, `Offline`, `Cancelled`, `Transport`) |

## Usage

```kotlin
val client = OkHttpApiClient(
  config = NetworkConfig(
    baseUrl = "https://httpbin.org",
    enableHttpLogging = true,
  ),
)
val response = client.perform(ApiRequest(url = "/get", method = HttpMethod.Get))
// Non-2xx still returns ApiResponse — check response.statusCode.
```

## Notes

- Relative URLs require `NetworkConfig.baseUrl`.
- `ApiRequest.timeoutMs` clones the OkHttp client for that call; prefer config-level timeouts.
- Deferred: SSL pinning, retry policy, token refresh, multipart, response interceptor chain.
