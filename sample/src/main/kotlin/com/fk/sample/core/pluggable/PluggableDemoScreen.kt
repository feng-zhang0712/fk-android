package com.fk.sample.core.pluggable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fk.core.pluggable.Pluggable
import com.fk.core.pluggable.PluggableServices
import com.fk.core.pluggable.logging.info
import com.fk.core.pluggable.mock.InMemoryKeyValueStore
import com.fk.core.pluggable.mock.MockApiClient
import com.fk.core.pluggable.mock.MockAppEnvironmentProvider
import com.fk.core.pluggable.mock.MockAppLifecycleObserver
import com.fk.core.pluggable.mock.MockCredentialStore
import com.fk.core.pluggable.mock.MockFeatureFlagProvider
import com.fk.core.pluggable.mock.MockPluggableLogger
import com.fk.core.pluggable.mock.MockReachability
import com.fk.core.pluggable.mock.MockRemoteConfigProvider
import com.fk.core.pluggable.mock.MockUserSession
import com.fk.core.pluggable.networking.ApiRequest
import com.fk.core.pluggable.networking.ApiResponse
import com.fk.core.pluggable.networking.HttpMethod
import com.fk.core.pluggable.storage.DefaultTypedStore
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

/**
 * Smoke demo for Phase A1 Pluggable contracts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluggableDemoScreen(
  onBack: () -> Unit,
) {
  val session = remember { MockUserSession(initiallyAuthenticated = false) }
  val storage = remember { DefaultTypedStore(InMemoryKeyValueStore()) }
  val apiClient = remember {
    MockApiClient().apply {
      setDefaultResponse(
        Result.success(
          ApiResponse(
            data = """{"ok":true}""".encodeToByteArray(),
            statusCode = 200,
          ),
        ),
      )
    }
  }
  val logger = remember { MockPluggableLogger() }
  val services = remember {
    PluggableServices(
      apiClient = apiClient,
      storage = storage,
      session = session,
      sessionObserver = session,
      environment = MockAppEnvironmentProvider(),
      featureFlags = MockFeatureFlagProvider(flags = mapOf("new_home" to true)),
      remoteConfig = MockRemoteConfigProvider(),
      logger = logger,
      reachability = MockReachability(isReachable = true),
      credentials = MockCredentialStore(),
      appLifecycle = MockAppLifecycleObserver(),
    )
  }

  DisposableEffect(Unit) {
    logger.info { "Pluggable sample started (contract=${Pluggable.CONTRACT_VERSION})" }
    onDispose { }
  }

  val scope = rememberCoroutineScope()
  var authenticated by remember { mutableStateOf(session.isAuthenticated) }
  var lastApi by remember { mutableStateOf("—") }
  var storedNote by remember {
    mutableStateOf(storage.get("demo.note", String.serializer()) ?: "(empty)")
  }

  DisposableEffect(session) {
    val token = session.observeAuthenticationChange { authenticated = it }
    onDispose { token.cancel() }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Pluggable") },
        navigationIcon = {
          TextButton(onClick = onBack) { Text("Back") }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Pluggable contract v${Pluggable.CONTRACT_VERSION}")
      HorizontalDivider()
      Text("Environment: ${services.environment?.environment}")
      Text("API base: ${services.environment?.apiBaseUrl}")
      Text("Reachable: ${services.reachability?.isReachable}")
      Text("Flag new_home: ${services.featureFlags?.isEnabled("new_home")}")
      Text("Session: authenticated=$authenticated userId=${session.userId}")
      Text("Storage note: $storedNote")
      Text("Last API: $lastApi")

      Button(
        onClick = {
          if (session.isAuthenticated) {
            session.signOut()
          } else {
            session.setAuthenticated(true, userId = "user-42")
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (authenticated) "Sign out" else "Sign in")
      }

      Button(
        onClick = {
          val value = "saved@${System.currentTimeMillis()}"
          storage.put("demo.note", value, String.serializer())
          storedNote = storage.get("demo.note", String.serializer()) ?: "(empty)"
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Write typed storage")
      }

      Button(
        onClick = {
          scope.launch {
            val response = apiClient.perform(
              ApiRequest(url = "https://api.example.com/ping", method = HttpMethod.Get),
            )
            lastApi = "HTTP ${response.statusCode}: ${response.data.decodeToString()}"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Mock API ping")
      }
    }
  }
}
