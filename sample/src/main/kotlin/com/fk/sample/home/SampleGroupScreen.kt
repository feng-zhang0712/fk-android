package com.fk.sample.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fk.sample.catalog.SampleCatalog
import com.fk.sample.catalog.SampleDestination
import com.fk.sample.catalog.SampleGroup
import com.fk.sample.ui.SampleTopBar

/**
 * Second-level list: demos belonging to a single [SampleGroup].
 */
@Composable
fun SampleGroupScreen(
  group: SampleGroup,
  onBack: () -> Unit,
  onOpen: (SampleDestination) -> Unit,
) {
  val destinations = SampleCatalog.destinationsIn(group)
  Scaffold(
    topBar = { SampleTopBar(title = group.title, onBack = onBack) },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = padding,
    ) {
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
              if (enabled) Modifier.clickable { onOpen(destination) } else Modifier,
            ),
        )
        HorizontalDivider()
      }
    }
  }
}
