package com.fk.sample.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fk.sample.catalog.SampleCatalog
import com.fk.sample.catalog.SampleDestination

/**
 * Root hub: grouped list of component demos (mirrors FKKitExamples hubs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleHomeScreen(
  onOpen: (SampleDestination) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("FK Android Samples") },
      )
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      SampleCatalog.grouped().forEach { (group, destinations) ->
        item(key = "header-${group.name}") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(group.title, style = MaterialTheme.typography.titleMedium)
            Text(
              group.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        items(destinations, key = { it.route }) { destination ->
          val enabled = destination.available
          ListItem(
            headlineContent = { Text(destination.title) },
            supportingContent = { Text(destination.description) },
            trailingContent = {
              if (!enabled) {
                Text(
                  "Soon",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.outline,
                )
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .then(
                if (enabled) {
                  Modifier.clickable { onOpen(destination) }
                } else {
                  Modifier
                },
              ),
          )
          HorizontalDivider()
        }
      }
    }
  }
}
