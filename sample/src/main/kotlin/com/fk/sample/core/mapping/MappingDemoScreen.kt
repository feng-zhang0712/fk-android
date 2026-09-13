package com.fk.sample.core.mapping

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
import androidx.compose.ui.unit.dp
import com.fk.core.mapping.Mapping
import com.fk.core.mapping.MappingException
import com.fk.core.mapping.Page
import com.fk.sample.ui.SampleTopBar
import kotlinx.serialization.Serializable

/**
 * Smoke demo for Phase A5 mapping (DTO encode/decode + envelope).
 */
@Composable
fun MappingDemoScreen(
  onBack: () -> Unit,
) {
  val mapper = remember { Mapping.apiMapper() }
  var status by remember { mutableStateOf("Idle") }
  var detail by remember { mutableStateOf("—") }

  Scaffold(
    topBar = { SampleTopBar(title = "Mapping", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Mapping package v${Mapping.VERSION}")
      Text("Preset: MappingJson.Api (snake_case, ignore unknowns)")
      HorizontalDivider()
      Text("Status: $status")
      Text("Detail:\n$detail")

      Button(
        onClick = {
          try {
            val json = """{"user_id":"u-1","display_name":"Ada"}"""
            val user = mapper.decode(json.toByteArray(), SampleUserDto.serializer())
            val encoded = mapper.encodeToString(user, SampleUserDto.serializer())
            status = "DTO round-trip OK"
            detail = "decoded=$user\nencoded=$encoded"
          } catch (e: MappingException) {
            status = e::class.simpleName ?: "MappingException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Encode/decode sample DTO")
      }

      Button(
        onClick = {
          try {
            val json = """
              {"code":0,"message":"ok","data":{"user_id":"u-2","display_name":"Grace"}}
            """.trimIndent()
            val user = mapper.decodeEnvelope(json.toByteArray(), SampleUserDto.serializer())
            status = "Envelope OK"
            detail = user.toString()
          } catch (e: MappingException) {
            status = e::class.simpleName ?: "MappingException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Decode success envelope")
      }

      Button(
        onClick = {
          try {
            val json = """{"code":401,"message":"unauthorized","data":null}"""
            mapper.decodeEnvelope(json.toByteArray(), SampleUserDto.serializer())
            status = "Unexpected success"
            detail = "—"
          } catch (e: MappingException.BusinessFailure) {
            status = "BusinessFailure (expected)"
            detail = "code=${e.code} message=${e.businessMessage}"
          } catch (e: MappingException) {
            status = e::class.simpleName ?: "MappingException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Decode failure envelope")
      }

      Button(
        onClick = {
          try {
            val json = """
              {"items":[{"user_id":"a","display_name":"A"}],"total":1,"page":1,"page_size":20}
            """.trimIndent()
            val page = mapper.decode(json.toByteArray(), Page.serializer(SampleUserDto.serializer()))
            status = "Page decode OK"
            detail = page.toString()
          } catch (e: MappingException) {
            status = e::class.simpleName ?: "MappingException"
            detail = e.message ?: "—"
          }
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Decode Page<T>")
      }
    }
  }
}

@Serializable
private data class SampleUserDto(
  val userId: String,
  val displayName: String,
)
