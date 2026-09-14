package com.fk.sample.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.list.ListConfiguration
import com.fk.ui.list.ListDataProvider
import com.fk.ui.list.ListFetchResult
import com.fk.ui.list.ListHost
import com.fk.ui.list.ListKit
import com.fk.ui.list.ListNoMoreDataBehavior
import com.fk.ui.list.ListPresentationState
import com.fk.ui.list.rememberListController
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle
import kotlinx.coroutines.delay

/**
 * Smoke demo for Phase E1 list (pull-to-refresh + empty + load-more).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListDemoScreen(
  onBack: () -> Unit,
) {
  val metrics = fkMetrics()
  var mode by remember { mutableStateOf(DemoMode.Feed) }
  val configuration = remember(mode) {
    ListConfiguration(
      skeletonRowCount = 6,
      noMoreDataBehavior = ListNoMoreDataBehavior.ShowFooter,
      emptyTitle = "No items",
      emptyDescription = "Switch mode or pull to refresh.",
    )
  }
  val provider = remember(mode) {
    DemoListProvider(mode)
  }
  val controller = rememberListController(
    configuration = configuration,
    provider = provider,
  )

  Scaffold(
    topBar = { SampleTopBar(title = "List", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      Text("ListKit package v${ListKit.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
      Text(
        "PTR + load-more + empty/skeleton orchestration",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      Text(
        statusLabel(controller.presentation, controller.items.size, controller.hasMore),
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
      )
      HorizontalDivider()

      Text("Scenario", style = fkTextStyle(FkTextStyle.Subheadline))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
      ) {
        DemoMode.entries.forEach { entry ->
          FilterChip(
            selected = mode == entry,
            onClick = { mode = entry },
            label = { Text(entry.label) },
          )
        }
      }
      HorizontalDivider()

      ListHost(
        controller = controller,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        key = { it.id },
      ) { _, item ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = metrics.spacingS),
        ) {
          Text(item.title, style = fkTextStyle(FkTextStyle.Body))
          Text(
            item.subtitle,
            style = fkTextStyle(FkTextStyle.Caption1),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
          )
        }
      }
    }
  }
}

private fun statusLabel(
  presentation: ListPresentationState,
  count: Int,
  hasMore: Boolean,
): String {
  val state = when (presentation) {
    ListPresentationState.InitialLoading -> "initialLoading"
    ListPresentationState.Content -> "content"
    ListPresentationState.Empty -> "empty"
    is ListPresentationState.Error -> "error"
    ListPresentationState.Refreshing -> "refreshing"
    ListPresentationState.LoadingNextPage -> "loadingNextPage"
  }
  return "state=$state · items=$count · hasMore=$hasMore"
}

private data class DemoItem(
  val id: String,
  val title: String,
  val subtitle: String,
)

private enum class DemoMode(val label: String) {
  Feed("Feed"),
  Empty("Empty"),
  FailInitial("Fail initial"),
  FailLoadMore("Fail page 2"),
}

private class DemoListProvider(
  private val mode: DemoMode,
) : ListDataProvider<DemoItem> {
  override suspend fun fetchInitial(page: Int): ListFetchResult<DemoItem> {
    delay(700)
    return when (mode) {
      DemoMode.Empty -> ListFetchResult(items = emptyList(), hasMorePages = false)
      DemoMode.FailInitial -> error("Simulated initial failure")
      DemoMode.Feed, DemoMode.FailLoadMore -> page(page = page, pageSize = 12)
    }
  }

  override suspend fun fetchRefresh(page: Int): ListFetchResult<DemoItem> {
    delay(600)
    return when (mode) {
      DemoMode.Empty -> ListFetchResult(items = emptyList(), hasMorePages = false)
      DemoMode.FailInitial -> error("Simulated refresh failure")
      DemoMode.Feed, DemoMode.FailLoadMore -> page(page = page, pageSize = 12)
    }
  }

  override suspend fun fetchNextPage(page: Int): ListFetchResult<DemoItem> {
    delay(650)
    if (mode == DemoMode.FailLoadMore && page >= 2) {
      error("Simulated load-more failure")
    }
    return page(page = page, pageSize = 12)
  }

  private fun page(page: Int, pageSize: Int): ListFetchResult<DemoItem> {
    val start = (page - 1) * pageSize
    val items = (0 until pageSize).map { offset ->
      val index = start + offset + 1
      DemoItem(
        id = "item-$index",
        title = "Item #$index",
        subtitle = "Page $page · pull to refresh · scroll for more",
      )
    }
    return ListFetchResult(
      items = items,
      hasMorePages = page < 3,
    )
  }
}
