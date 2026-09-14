package com.fk.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fk.ui.empty.EmptyAction
import com.fk.ui.empty.EmptyActionSet
import com.fk.ui.empty.EmptyConfiguration
import com.fk.ui.empty.EmptyContent
import com.fk.ui.empty.EmptyPhase
import com.fk.ui.empty.EmptyStateContent
import com.fk.ui.empty.EmptyType
import com.fk.ui.skeleton.SkeletonContainer
import com.fk.ui.skeleton.SkeletonListPlaceholder
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Lazy list host with pull-to-refresh, load-more footer, and empty/skeleton orchestration.
 *
 * Conceptually aligned with iOS ListKit table/collection controllers + FKRefresh chrome
 * (scenario port — not a DiffableDataSource).
 *
 * @param skeleton Optional override for [ListPresentationState.InitialLoading]
 *   (default: [SkeletonListPlaceholder] using [ListConfiguration.skeletonRowCount]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ListHost(
  controller: ListController<T>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  key: ((item: T) -> Any)? = null,
  emptyConfiguration: EmptyConfiguration? = null,
  onEmptyAction: (EmptyAction) -> Unit = { action ->
    if (action.id == "retry" || action.id == "primary") {
      controller.retry()
    }
  },
  skeleton: (@Composable () -> Unit)? = null,
  itemContent: @Composable (index: Int, item: T) -> Unit,
) {
  val configuration = controller.configuration

  val body: @Composable () -> Unit = {
    when (val state = controller.presentation) {
      ListPresentationState.InitialLoading -> {
        if (skeleton != null) {
          skeleton()
        } else {
          SkeletonContainer(modifier = Modifier.fillMaxSize()) {
            SkeletonListPlaceholder(
              count = configuration.skeletonRowCount,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
      is ListPresentationState.Error -> {
        val config = (emptyConfiguration ?: EmptyConfiguration()).copy(
          phase = EmptyPhase.Error,
          type = EmptyType.Error,
          content = EmptyContent(
            title = state.title,
            description = state.message,
          ),
          actions = EmptyActionSet.primary(
            title = EmptyConfiguration.DefaultRetryTitle,
            id = "retry",
          ),
        )
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          EmptyStateContent(configuration = config, onAction = onEmptyAction)
        }
      }
      ListPresentationState.Empty -> {
        val config = (emptyConfiguration ?: EmptyConfiguration(
          content = EmptyContent(
            title = configuration.emptyTitle,
            description = configuration.emptyDescription,
          ),
          actions = EmptyActionSet.primary("Retry", id = "retry"),
        )).copy(phase = EmptyPhase.Empty)
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          EmptyStateContent(configuration = config, onAction = onEmptyAction)
        }
      }
      ListPresentationState.Content,
      ListPresentationState.Refreshing,
      ListPresentationState.LoadingNextPage,
      -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          state = listState,
          contentPadding = contentPadding,
        ) {
          itemsIndexed(
            items = controller.items,
            key = if (key != null) {
              { _, item -> key(item) }
            } else {
              null
            },
          ) { index, item ->
            itemContent(index, item)
          }
          if (configuration.loadMoreEnabled) {
            item(key = "fk_list_load_more_footer") {
              ListLoadMoreFooter(
                state = controller.loadMoreState,
                noMoreDataBehavior = configuration.noMoreDataBehavior,
                onRetry = { controller.loadMore() },
                onManualLoad = { controller.loadMore() },
                showManualTrigger =
                  configuration.loadMoreTrigger == ListLoadMoreTrigger.Manual &&
                    controller.hasMore &&
                    controller.loadMoreState is ListLoadMoreState.Idle,
              )
            }
          }
        }
      }
    }
  }

  if (configuration.pullToRefreshEnabled) {
    PullToRefreshBox(
      isRefreshing = controller.isRefreshing,
      onRefresh = { controller.refresh() },
      modifier = modifier.fillMaxSize(),
    ) {
      body()
    }
  } else {
    Box(modifier = modifier.fillMaxSize()) {
      body()
    }
  }

  if (
    configuration.loadMoreEnabled &&
    configuration.loadMoreTrigger == ListLoadMoreTrigger.Automatic
  ) {
    ListLoadMoreEffect(
      controller = controller,
      listState = listState,
      preloadItemCount = configuration.preloadItemCount,
    )
  }
}

@Composable
private fun <T> ListLoadMoreEffect(
  controller: ListController<T>,
  listState: LazyListState,
  preloadItemCount: Int,
) {
  val shouldLoad by remember(listState, controller.items.size, controller.hasMore) {
    derivedStateOf {
      val total = controller.items.size
      if (total == 0 || !controller.hasMore) return@derivedStateOf false
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        ?: return@derivedStateOf false
      lastVisible >= (total - 1 - preloadItemCount.coerceAtLeast(0))
    }
  }
  LaunchedEffect(shouldLoad, controller.presentation, controller.loadMoreState, controller.isRefreshing) {
    if (!shouldLoad) return@LaunchedEffect
    if (controller.isRefreshing) return@LaunchedEffect
    if (controller.presentation !is ListPresentationState.Content &&
      controller.presentation !is ListPresentationState.LoadingNextPage
    ) {
      return@LaunchedEffect
    }
    if (controller.loadMoreState is ListLoadMoreState.Loading) return@LaunchedEffect
    if (controller.loadMoreState is ListLoadMoreState.Failed) return@LaunchedEffect
    if (controller.loadMoreState is ListLoadMoreState.NoMore) return@LaunchedEffect
    controller.loadMore()
  }
}

/**
 * Load-more footer chrome (loading / failed / no-more / manual).
 *
 * Renders nothing (no reserved space) when idle without a manual trigger, or when
 * [ListNoMoreDataBehavior.HideFooter] applies.
 */
@Composable
fun ListLoadMoreFooter(
  state: ListLoadMoreState,
  modifier: Modifier = Modifier,
  noMoreDataBehavior: ListNoMoreDataBehavior = ListNoMoreDataBehavior.HideFooter,
  onRetry: () -> Unit = {},
  onManualLoad: () -> Unit = {},
  showManualTrigger: Boolean = false,
) {
  val visible = when (state) {
    ListLoadMoreState.Idle -> showManualTrigger
    ListLoadMoreState.NoMore -> noMoreDataBehavior == ListNoMoreDataBehavior.ShowFooter
    ListLoadMoreState.Loading,
    is ListLoadMoreState.Failed,
    -> true
  }
  if (!visible) return

  val metrics = fkMetrics()
  val secondary = fkColor(FkColorRole.OnSurfaceSecondary)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = metrics.spacingM),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
  ) {
    when (state) {
      ListLoadMoreState.Idle -> {
        TextButton(onClick = onManualLoad) {
          Text("Load more")
        }
      }
      ListLoadMoreState.Loading -> {
        CircularProgressIndicator(
          modifier = Modifier.padding(metrics.spacingS),
          strokeWidth = 2.dp,
          color = secondary,
        )
      }
      ListLoadMoreState.NoMore -> {
        Text(
          text = "No more data",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = secondary,
          textAlign = TextAlign.Center,
        )
      }
      is ListLoadMoreState.Failed -> {
        Text(
          text = state.message ?: ListKit.DefaultLoadMoreError,
          style = fkTextStyle(FkTextStyle.Caption1),
          color = secondary,
          textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
          Text("Retry")
        }
      }
    }
  }
}
