package com.fk.sample.core.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.pluggable.storage.DefaultTypedStore
import com.fk.core.pluggable.storage.TypedStore
import com.fk.core.storage.Storage
import com.fk.core.storage.StorageException
import com.fk.core.storage.StringStorageKey
import com.fk.sample.ui.SampleTopBar
import kotlinx.serialization.builtins.serializer

/**
 * Smoke demo for Phase A3 DataStore + Keystore-encrypted storage.
 */
@Composable
fun StorageDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val plainKv = remember {
    Storage.keyValueStore(context, name = "fk_sample_storage")
  }
  val plain = remember { DefaultTypedStore(plainKv) }
  val secrets: TypedStore = remember {
    Storage.encryptedTypedStore(context, name = "fk_sample_storage_secure")
  }

  val noteKey = remember { StringStorageKey(namespace = "sample", rawValue = "note") }
  val tokenKey = remember { StringStorageKey(namespace = "sample", rawValue = "token") }

  var draft by remember { mutableStateOf("hello DataStore") }
  var secretDraft by remember { mutableStateOf("super-secret-token") }
  var status by remember { mutableStateOf("Idle") }
  var plainValue by remember {
    mutableStateOf(plain.get(noteKey.fullKey, String.serializer()) ?: "(empty)")
  }
  var secretValue by remember {
    mutableStateOf(readSecretPreview(secrets, tokenKey.fullKey))
  }
  var ownedKeys by remember { mutableStateOf(plainKv.ownedKeys().joinToString()) }

  Scaffold(
    topBar = { SampleTopBar(title = "Storage", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Storage package v${Storage.VERSION}")
      Text("Plain: DataStoreKeyValueStore → TypedStore")
      Text("Secure: EncryptedKeyValueStore (Keystore AES-GCM)")
      HorizontalDivider()
      Text("Status: $status")
      Text("Plain value: $plainValue")
      Text("Secret preview: $secretValue")
      Text("Owned keys: ${ownedKeys.ifBlank { "(none)" }}")

      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Plain note") },
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {
          try {
            plain.put(noteKey.fullKey, draft, String.serializer())
            plainValue = plain.get(noteKey.fullKey, String.serializer()) ?: "(empty)"
            ownedKeys = plainKv.ownedKeys().joinToString()
            status = "Plain write OK"
          } catch (e: StorageException) {
            status = e::class.simpleName ?: "StorageException"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Write + read plain")
      }

      OutlinedTextField(
        value = secretDraft,
        onValueChange = { secretDraft = it },
        label = { Text("Secret token") },
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {
          try {
            secrets.put(tokenKey.fullKey, secretDraft, String.serializer())
            secretValue = readSecretPreview(secrets, tokenKey.fullKey)
            status = "Encrypted write OK"
          } catch (e: StorageException) {
            status = e::class.simpleName ?: "StorageException"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Write + read encrypted")
      }

      Button(
        onClick = {
          try {
            plainKv.clearOwned()
            plainValue = plain.get(noteKey.fullKey, String.serializer()) ?: "(empty)"
            ownedKeys = plainKv.ownedKeys().joinToString()
            status = "Plain store cleared"
          } catch (e: StorageException) {
            status = e::class.simpleName ?: "StorageException"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Clear plain owned keys")
      }
    }
  }
}

private fun readSecretPreview(
  secrets: TypedStore,
  key: String,
): String {
  val value = secrets.get(key, String.serializer()) ?: return "(empty)"
  if (value.length <= 4) return "••••"
  return "••••" + value.takeLast(4)
}
