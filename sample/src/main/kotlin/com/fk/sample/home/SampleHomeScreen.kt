package com.fk.sample.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fk.sample.catalog.SampleGroup
import com.fk.sample.ui.SampleTopBar

/**
 * Top-level hub: only Core / UI / Business entries.
 */
@Composable
fun SampleHomeScreen(
  onOpenGroup: (SampleGroup) -> Unit,
) {
  Scaffold(
    topBar = { SampleTopBar(title = "FK Android Samples") },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = padding,
    ) {
      items(SampleGroup.entries, key = { it.name }) { group ->
        ListItem(
          headlineContent = { Text(group.title) },
          supportingContent = { Text(group.subtitle) },
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGroup(group) },
        )
        HorizontalDivider()
      }
    }
  }
}
