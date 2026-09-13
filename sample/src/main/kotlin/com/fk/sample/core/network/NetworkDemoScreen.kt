package com.fk.sample.core.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fk.core.network.Network
import com.fk.core.network.NetworkConfig
import com.fk.core.network.NetworkException
import com.fk.core.network.OkHttpApiClient
import com.fk.core.pluggable.networking.ApiRequest
import com.fk.core.pluggable.networking.HttpMethod
import com.fk.sample.ui.SampleTopBar
import kotlinx.coroutines.launch

/**
 * Smoke demo for Phase A2 OkHttp [OkHttpApiClient].
 */
@Composable
fun NetworkDemoScreen(
  onBack: () -> Unit,
) {
  val client = remember {
    OkHttpApiClient(
      config = NetworkConfig(
        baseUrl = "https://httpbin.org",
        enableHttpLogging = true,
        defaultHeaders = mapOf("Accept" to "application/json"),
      ),
    )
  }
  val scope = rememberCoroutineScope()
  var status by remember { mutableStateOf("Idle") }
  var bodyPreview by remember { mutableStateOf("—") }

  Scaffold(
    topBar = { SampleTopBar(title = "Network", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Network package v${Network.VERSION}")
      Text("Client: OkHttpApiClient → Pluggable ApiClient")
      HorizontalDivider()
      Text("Status: $status")
      Text("Body preview:\n$bodyPreview")

      Button(
        onClick = {
          scope.launch {
            status = "Loading…"
            try {
              val response = client.perform(
                ApiRequest(url = "/get", method = HttpMethod.Get),
              )
              status = "HTTP ${response.statusCode}"
              bodyPreview = response.data.decodeToString().take(400)
            } catch (e: NetworkException) {
              status = e::class.simpleName ?: "NetworkException"
              val cause = e.cause?.message
              bodyPreview = buildString {
                append(e.message ?: "—")
                if (!cause.isNullOrBlank()) {
                  append("\nCause: ")
                  append(cause)
                }
              }
            } catch (e: Exception) {
              status = "Error"
              bodyPreview = e.message ?: e.toString()
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("GET httpbin.org/get")
      }
    }
  }
}
