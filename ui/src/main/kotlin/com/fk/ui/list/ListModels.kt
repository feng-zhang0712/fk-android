package com.fk.ui.list

/**
 * List orchestration package hub — refresh, load-more, empty/skeleton wiring.
 *
 * Conceptually aligned with iOS `FKListKit` + `FKRefresh` (Compose scenario port).
 * Named [ListKit] to avoid clashing with `kotlin.collections.List`.
 */
object ListKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"

  /** Default empty-state title when the list has zero items. */
  const val DefaultEmptyTitle: String = "Nothing here yet"

  /** Default empty-state description. */
  const val DefaultEmptyDescription: String = "Pull to refresh or try again."

  /** Default error title for failed initial loads. */
  const val DefaultErrorTitle: String = "Something went wrong"

  /** Default error description. */
  const val DefaultErrorDescription: String = "We couldn't load this content."

  /** Default load-more failure message. */
  const val DefaultLoadMoreError: String = "Couldn't load more"
}

/**
 * High-level list UI state driving skeleton, empty, refresh, and content visibility.
 *
 * Conceptually aligned with iOS `FKListPresentationState`.
 */
sealed class ListPresentationState {
  /** First paint / reload while the item list is empty. */
  data object InitialLoading : ListPresentationState()

  /** Non-empty content is visible. */
  data object Content : ListPresentationState()

  /** Successful load with zero items. */
  data object Empty : ListPresentationState()

  /** Failed load that should surface an error empty-state. */
  data class Error(
    val title: String = ListKit.DefaultErrorTitle,
    val message: String? = ListKit.DefaultErrorDescription,
  ) : ListPresentationState()

  /** Pull-to-refresh in flight (content may still be visible). */
  data object Refreshing : ListPresentationState()

  /** Footer load-more in flight. */
  data object LoadingNextPage : ListPresentationState()
}

/**
 * Result of a page fetch.
 *
 * Conceptually aligned with iOS `FKListFetchResult` (items + hasMore, no Diffable snapshot).
 */
data class ListFetchResult<T>(
  val items: List<T>,
  val hasMorePages: Boolean,
)

/**
 * 1-based page index tracker for list APIs.
 *
 * Conceptually aligned with iOS `FKRefreshPagination`.
 */
data class ListPagination(
  val page: Int = 1,
) {
  init {
    require(page >= 1) { "page must be >= 1" }
  }

  /** Call when the user pulls to refresh — resets to the first page. */
  fun resetForNewRequest(): ListPagination = copy(page = 1)

  /** Call after a successful load-more response. */
  fun advance(): ListPagination = copy(page = page + 1)

  /** Page index to request on the next load-more fetch. */
  val nextPage: Int get() = page + 1
}

/**
 * Footer chrome for pagination.
 */
sealed class ListLoadMoreState {
  data object Idle : ListLoadMoreState()
  data object Loading : ListLoadMoreState()
  data object NoMore : ListLoadMoreState()
  data class Failed(val message: String? = ListKit.DefaultLoadMoreError) : ListLoadMoreState()
}

/**
 * When to trigger automatic load-more.
 *
 * Conceptually aligned with iOS `FKLoadMoreTriggerMode`.
 */
enum class ListLoadMoreTrigger {
  /** Fire when the user scrolls near the end of the list. */
  Automatic,

  /** Only via [ListController.loadMore] / footer retry. */
  Manual,
}

/**
 * Whether to keep a “no more data” footer visible.
 *
 * Conceptually aligned with iOS `FKRefreshNoMoreDataBehavior`.
 */
enum class ListNoMoreDataBehavior {
  ShowFooter,
  HideFooter,
}

/**
 * Aggregate list host configuration.
 *
 * Conceptually aligned with a narrow subset of iOS `FKListConfiguration` / refresh flags.
 */
data class ListConfiguration(
  val pullToRefreshEnabled: Boolean = true,
  val loadMoreEnabled: Boolean = true,
  val loadMoreTrigger: ListLoadMoreTrigger = ListLoadMoreTrigger.Automatic,
  /** Trigger load-more when the last visible index is within this many items of the end. */
  val preloadItemCount: Int = 3,
  val clearsItemsOnRefreshStart: Boolean = false,
  val refreshFailureKeepsContent: Boolean = true,
  val cancelLoadMoreOnRefresh: Boolean = true,
  val noMoreDataBehavior: ListNoMoreDataBehavior = ListNoMoreDataBehavior.HideFooter,
  /** Skeleton placeholder row count for [ListPresentationState.InitialLoading]. */
  val skeletonRowCount: Int = 8,
  val emptyTitle: String = ListKit.DefaultEmptyTitle,
  val emptyDescription: String? = ListKit.DefaultEmptyDescription,
  val errorTitle: String = ListKit.DefaultErrorTitle,
  val errorDescription: String? = ListKit.DefaultErrorDescription,
)

/**
 * Data contract for list page fetches.
 *
 * Conceptually aligned with iOS `FKListDataProviding`.
 */
interface ListDataProvider<T> {
  suspend fun fetchInitial(page: Int): ListFetchResult<T>

  suspend fun fetchRefresh(page: Int): ListFetchResult<T> = fetchInitial(page)

  suspend fun fetchNextPage(page: Int): ListFetchResult<T>
}
