package com.fk.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Orchestrates presentation state, pagination, refresh, and load-more for Lazy lists.
 *
 * Conceptually aligned with iOS ListKit load / presentation coordinators (narrow Compose port).
 *
 * Call [dispose] when the host leaves composition (see [rememberListController]).
 */
@Stable
class ListController<T>(
  val configuration: ListConfiguration = ListConfiguration(),
  private val scope: CoroutineScope,
) {
  /** Current items rendered by [ListHost]. */
  var items: List<T> by mutableStateOf(emptyList())
    private set

  /** High-level UI state for skeleton / empty / content chrome. */
  var presentation: ListPresentationState by mutableStateOf(ListPresentationState.InitialLoading)
    private set

  /** 1-based pagination tracker. */
  var pagination: ListPagination by mutableStateOf(ListPagination())
    private set

  /** Whether another page may be requested. */
  var hasMore: Boolean by mutableStateOf(true)
    private set

  /** Pull-to-refresh indicator flag. */
  var isRefreshing: Boolean by mutableStateOf(false)
    private set

  /** Footer load-more chrome. */
  var loadMoreState: ListLoadMoreState by mutableStateOf(ListLoadMoreState.Idle)
    private set

  private var provider: ListDataProvider<T>? = null
  private var generation: Int = 0
  private var initialJob: Job? = null
  private var refreshJob: Job? = null
  private var loadMoreJob: Job? = null
  private var disposed: Boolean = false

  /** Binds a [ListDataProvider] used by [loadInitial], [refresh], and [loadMore]. */
  fun bind(provider: ListDataProvider<T>): ListController<T> {
    this.provider = provider
    return this
  }

  /**
   * Starts the initial fetch (page 1).
   *
   * Shows skeleton while [items] is empty.
   */
  fun loadInitial() {
    val data = provider ?: return
    if (disposed) return
    cancelJobs(cancelRefresh = true, cancelLoadMore = true, cancelInitial = true)
    val token = ++generation
    pagination = ListPagination()
    hasMore = true
    loadMoreState = ListLoadMoreState.Idle
    if (items.isEmpty()) {
      presentation = ListPresentationState.InitialLoading
    }
    initialJob = scope.launch {
      try {
        val result = data.fetchInitial(page = 1)
        if (!isCurrent(token)) return@launch
        applyReplace(result)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (!isCurrent(token)) return@launch
        applyInitialFailure(error)
      }
    }
  }

  /** Pull-to-refresh (resets pagination to page 1). */
  fun refresh() {
    val data = provider ?: return
    if (disposed) return
    if (isRefreshing) return
    if (configuration.cancelLoadMoreOnRefresh) {
      cancelLoadMore()
    }
    val token = ++generation
    isRefreshing = true
    presentation = ListPresentationState.Refreshing
    pagination = pagination.resetForNewRequest()
    if (configuration.clearsItemsOnRefreshStart) {
      // Keep presentation as Refreshing so empty chrome does not flash.
      items = emptyList()
    }
    refreshJob = scope.launch {
      try {
        val result = data.fetchRefresh(page = 1)
        if (!isCurrent(token)) return@launch
        applyReplace(result)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (!isCurrent(token)) return@launch
        applyRefreshFailure(error)
      } finally {
        if (isCurrent(token)) {
          isRefreshing = false
        }
      }
    }
  }

  /**
   * Requests the next page when allowed.
   *
   * No-ops when not in a content-capable state, already loading, or [hasMore] is false.
   */
  fun loadMore() {
    val data = provider ?: return
    if (disposed) return
    if (!configuration.loadMoreEnabled) return
    if (isRefreshing) return
    if (presentation is ListPresentationState.Refreshing) return
    if (!hasMore) {
      loadMoreState = ListLoadMoreState.NoMore
      return
    }
    if (loadMoreState is ListLoadMoreState.Loading) return
    if (loadMoreJob?.isActive == true) return
    when (presentation) {
      ListPresentationState.Content,
      ListPresentationState.LoadingNextPage,
      -> Unit
      else -> return
    }

    val page = pagination.nextPage
    val token = generation
    loadMoreState = ListLoadMoreState.Loading
    presentation = ListPresentationState.LoadingNextPage
    loadMoreJob = scope.launch {
      try {
        val result = data.fetchNextPage(page)
        if (!isCurrent(token) || disposed) return@launch
        items = items + result.items
        hasMore = result.hasMorePages
        pagination = pagination.advance()
        loadMoreState = if (result.hasMorePages) {
          ListLoadMoreState.Idle
        } else {
          ListLoadMoreState.NoMore
        }
        presentation = contentOrEmpty(items)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        if (!isCurrent(token) || disposed) return@launch
        loadMoreState = ListLoadMoreState.Failed(
          message = error.message?.takeIf { it.isNotBlank() } ?: ListKit.DefaultLoadMoreError,
        )
        // Footer failure returns to content — do not flip the whole screen to Error.
        presentation = contentOrEmpty(items)
      }
    }
  }

  /**
   * Retries based on the current failure surface (full-screen error or load-more footer).
   */
  fun retry() {
    when {
      loadMoreState is ListLoadMoreState.Failed -> loadMore()
      else -> loadInitial()
    }
  }

  /** Replaces items without a network round-trip (tests / optimistic updates). */
  fun replaceItems(newItems: List<T>, hasMorePages: Boolean = hasMore) {
    items = newItems
    hasMore = hasMorePages
    loadMoreState = if (hasMorePages) ListLoadMoreState.Idle else ListLoadMoreState.NoMore
    presentation = contentOrEmpty(newItems)
  }

  /**
   * Appends items without a network round-trip (e.g. realtime push).
   *
   * Does not advance [pagination]; keep paging via [loadMore] on a separate path.
   */
  fun appendItems(more: List<T>, hasMorePages: Boolean = hasMore) {
    if (more.isNotEmpty()) {
      items = items + more
    }
    hasMore = hasMorePages
    loadMoreState = if (hasMorePages) ListLoadMoreState.Idle else ListLoadMoreState.NoMore
    presentation = contentOrEmpty(items)
  }

  /** Clears jobs; safe to call multiple times. */
  fun dispose() {
    if (disposed) return
    disposed = true
    cancelJobs(cancelRefresh = true, cancelLoadMore = true, cancelInitial = true)
    provider = null
  }

  private fun applyReplace(result: ListFetchResult<T>) {
    items = result.items
    hasMore = result.hasMorePages
    pagination = ListPagination(page = 1)
    loadMoreState = if (result.hasMorePages) {
      ListLoadMoreState.Idle
    } else {
      ListLoadMoreState.NoMore
    }
    presentation = contentOrEmpty(result.items)
  }

  private fun applyInitialFailure(error: Throwable) {
    val message = error.message?.takeIf { it.isNotBlank() }
    if (items.isNotEmpty()) {
      presentation = ListPresentationState.Content
      return
    }
    presentation = ListPresentationState.Error(
      title = configuration.errorTitle,
      message = message ?: configuration.errorDescription,
    )
  }

  private fun applyRefreshFailure(error: Throwable) {
    val message = error.message?.takeIf { it.isNotBlank() }
    if (configuration.refreshFailureKeepsContent && items.isNotEmpty()) {
      presentation = ListPresentationState.Content
      return
    }
    presentation = if (items.isEmpty()) {
      ListPresentationState.Error(
        title = configuration.errorTitle,
        message = message ?: configuration.errorDescription,
      )
    } else {
      ListPresentationState.Content
    }
  }

  private fun cancelLoadMore() {
    loadMoreJob?.cancel()
    loadMoreJob = null
    if (loadMoreState is ListLoadMoreState.Loading) {
      loadMoreState = ListLoadMoreState.Idle
    }
    if (presentation is ListPresentationState.LoadingNextPage) {
      presentation = contentOrEmpty(items)
    }
  }

  private fun cancelJobs(
    cancelRefresh: Boolean,
    cancelLoadMore: Boolean,
    cancelInitial: Boolean,
  ) {
    if (cancelInitial) {
      initialJob?.cancel()
      initialJob = null
    }
    if (cancelRefresh) {
      refreshJob?.cancel()
      refreshJob = null
      isRefreshing = false
    }
    if (cancelLoadMore) {
      cancelLoadMore()
    }
  }

  private fun isCurrent(token: Int): Boolean = !disposed && token == generation

  private fun contentOrEmpty(current: List<T>): ListPresentationState =
    if (current.isEmpty()) ListPresentationState.Empty else ListPresentationState.Content
}

/**
 * Remembers a [ListController] bound to [provider] and optionally loads initial content.
 */
@Composable
fun <T> rememberListController(
  configuration: ListConfiguration = ListConfiguration(),
  provider: ListDataProvider<T>,
  loadOnStart: Boolean = true,
): ListController<T> {
  val scope = rememberCoroutineScope()
  val controller = remember(configuration) {
    ListController<T>(configuration = configuration, scope = scope)
  }
  DisposableEffect(controller) {
    onDispose { controller.dispose() }
  }
  LaunchedEffect(controller, provider, loadOnStart) {
    controller.bind(provider)
    if (loadOnStart) {
      controller.loadInitial()
    }
  }
  return controller
}
