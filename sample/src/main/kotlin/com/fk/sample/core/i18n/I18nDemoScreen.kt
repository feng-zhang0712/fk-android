package com.fk.sample.core.i18n

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fk.core.i18n.I18n
import com.fk.core.i18n.I18nKey
import com.fk.core.i18n.MessageFormat
import com.fk.core.i18n.NumberStyle
import com.fk.sample.ui.SampleTopBar
import java.util.Date

/**
 * Smoke demo for Phase B2 i18n (locale switch + dictionary + formatters).
 */
@Composable
fun I18nDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val manager = remember {
    I18n.dictionaryManager(
      flatDictionary = mapOf(
        "en" to mapOf(
          "demo.title" to "I18n demo",
          "demo.greeting" to "Hello, {name}",
          "demo.items" to "1 item|{count} items",
        ),
        "zh-Hans" to mapOf(
          "demo.title" to "国际化演示",
          "demo.greeting" to "你好，{name}",
          "demo.items" to "1 项|{count} 项",
        ),
      ),
      context = context.applicationContext,
    )
  }

  var languageCode by remember { mutableStateOf(manager.currentLanguageCode) }
  var title by remember { mutableStateOf("") }
  var greeting by remember { mutableStateOf("") }
  var plural by remember { mutableStateOf("") }
  var formatted by remember { mutableStateOf("") }

  fun refreshCopy() {
    languageCode = manager.currentLanguageCode
    title = manager.localized(I18nKey("demo.title"))
    greeting = manager.localized("demo.greeting", mapOf("name" to "FK"))
    plural = MessageFormat.interpolate(
      manager.localizedPlural("demo.items", count = 3),
      mapOf("count" to "3"),
    )
    val fmt = manager.formatters
    formatted = buildString {
      append("number=").append(fmt.formatNumber(12345.67))
      append("\ndate=").append(fmt.formatDate(Date()))
      append("\npercent=").append(fmt.formatNumber(0.42, NumberStyle.Percent))
    }
  }

  DisposableEffect(manager) {
    val token = manager.observeLanguageChange { refreshCopy() }
    onDispose { token.cancel() }
  }

  Scaffold(
    topBar = { SampleTopBar(title = "I18n", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("I18n package v${I18n.VERSION}")
      Text("Active: $languageCode · RTL=${manager.isRightToLeft}")
      HorizontalDivider()
      Text(title)
      Text(greeting)
      Text(plural)
      Text(formatted)

      Button(
        onClick = { manager.setLanguageCode("en") },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Switch to English (en)")
      }

      Button(
        onClick = { manager.setLanguageCode("zh-Hans") },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Switch to 简体中文 (zh-Hans)")
      }

      Button(
        onClick = { manager.resetLanguageSelection() },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Reset to default language")
      }
    }
  }
}
