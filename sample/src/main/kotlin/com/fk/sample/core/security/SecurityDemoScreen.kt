package com.fk.sample.core.security

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.security.AesMode
import com.fk.core.security.HashAlgorithm
import com.fk.core.security.Security
import com.fk.core.security.SecurityException
import com.fk.sample.ui.SampleTopBar

/**
 * Smoke demo for Phase B1 security (AES round-trip + hash / mask / key store).
 */
@Composable
fun SecurityDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val security = remember { Security.create(context) }
  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }

  Scaffold(
    topBar = { SampleTopBar(title = "Security", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Security package v${Security.VERSION}")
      Text("AES-CBC · SHA-256 · HMAC · Keystore-wrapped key store")
      HorizontalDivider()
      Text("Status: $status")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          try {
            val key = security.aes.generateKey()
            val iv = security.aes.generateIv()
            val cipher = security.aes.encryptToBase64("hello FKSecurity", key, iv, AesMode.Cbc)
            val plain = security.aes.decryptFromBase64(cipher, key, iv, AesMode.Cbc)
            status = "AES round-trip OK"
            detail = "cipher=${cipher.take(48)}…\nplain=$plain"
          } catch (e: SecurityException) {
            status = e::class.simpleName ?: "SecurityException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("AES encrypt/decrypt round-trip")
      }

      Button(
        onClick = {
          try {
            val digest = security.hasher.hashString("fk-android", HashAlgorithm.Sha256)
            val masked = security.utils.maskPhone("13812345678")
            status = "Hash + mask OK"
            detail = "sha256=$digest\nphone=$masked"
          } catch (e: SecurityException) {
            status = e::class.simpleName ?: "SecurityException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("SHA-256 + mask phone")
      }

      Button(
        onClick = {
          try {
            val store = security.secretKeyStore
              ?: error("secretKeyStore missing")
            val key = security.aes.generateKey()
            store.put("demo.aes", key)
            val restored = store.get("demo.aes")
            val match = key.contentEquals(restored)
            store.remove("demo.aes")
            status = if (match) "Keystore key store OK" else "Mismatch"
            detail = "stored=${key.size} bytes, restored match=$match"
          } catch (e: SecurityException) {
            status = e::class.simpleName ?: "SecurityException"
            detail = e.message ?: "—"
          } catch (e: Exception) {
            status = "Error"
            detail = e.message ?: e.toString()
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Keystore-backed key store round-trip")
      }

      Button(
        onClick = {
          try {
            val pair = security.rsa.generateKeyPair(keySizeBits = 2048, tag = "demo")
            val message = "sign-me".toByteArray()
            val signature = security.rsa.sign(message, pair.privateKeyDer)
            val ok = security.rsa.verify(signature, message, pair.publicKeyDer)
            status = if (ok) "RSA sign/verify OK" else "RSA verify failed"
            detail = "pub=${pair.publicKeyDer.size}B priv=${pair.privateKeyDer.size}B"
          } catch (e: SecurityException) {
            status = e::class.simpleName ?: "SecurityException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("RSA sign/verify")
      }
    }
  }
}
