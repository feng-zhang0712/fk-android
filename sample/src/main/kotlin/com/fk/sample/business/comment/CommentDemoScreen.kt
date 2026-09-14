package com.fk.sample.business.comment

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.fk.business.comment.CommentConfiguration
import com.fk.business.comment.CommentItem
import com.fk.business.comment.CommentKit
import com.fk.business.comment.CommentListController
import com.fk.business.comment.CommentListDataSource
import com.fk.business.comment.CommentListHost
import com.fk.business.comment.CommentListListener
import com.fk.business.comment.CommentListPage
import com.fk.business.comment.CommentMoreAction
import com.fk.business.comment.CommentReplyTarget
import com.fk.business.comment.CommentSubmitRequest
import com.fk.business.comment.rememberCommentListController
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private enum class CommentDemoDestination {
  Hub,
  StandardLongList,
  CompactLongList,
}

/**
 * Comment kit sample: hub with one Long-list demo per layout preset.
 *
 * Each long list exercises like (incl. rollback), reply / expand, more menu,
 * load-more, verified / owned rows, and expandable body.
 */
@Composable
fun CommentDemoScreen(
  onBack: () -> Unit,
) {
  var destination by remember { mutableStateOf(CommentDemoDestination.Hub) }
  when (destination) {
    CommentDemoDestination.Hub -> CommentDemoHub(
      onBack = onBack,
      onOpenStandard = { destination = CommentDemoDestination.StandardLongList },
      onOpenCompact = { destination = CommentDemoDestination.CompactLongList },
    )
    CommentDemoDestination.StandardLongList -> CommentLongListScreen(
      title = "Standard · Long list",
      configuration = CommentConfiguration.standard(),
      compactStyle = false,
      onBack = { destination = CommentDemoDestination.Hub },
    )
    CommentDemoDestination.CompactLongList -> CommentLongListScreen(
      title = "Compact · Long list",
      configuration = CommentConfiguration.compact(),
      compactStyle = true,
      onBack = { destination = CommentDemoDestination.Hub },
    )
  }
}

@Composable
private fun CommentDemoHub(
  onBack: () -> Unit,
  onOpenStandard: () -> Unit,
  onOpenCompact: () -> Unit,
) {
  val metrics = fkMetrics()
  Scaffold(
    topBar = {
      SampleTopBar(
        title = "Comment v${CommentKit.VERSION}",
        onBack = onBack,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = "Layout presets",
        style = fkTextStyle(FkTextStyle.Footnote).copy(fontWeight = FontWeight.SemiBold),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
        modifier = Modifier.padding(
          horizontal = metrics.spacingM,
          vertical = metrics.spacingS,
        ),
      )
      ListItem(
        headlineContent = { Text("Standard") },
        supportingContent = {
          Text("Feed-style row + flat composer. Long list with all interactions.")
        },
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenStandard),
      )
      HorizontalDivider()
      ListItem(
        headlineContent = { Text("Compact") },
        supportingContent = {
          Text("Author ▸ reply-to + meta rail + capsule composer. Long list with all interactions.")
        },
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenCompact),
      )
    }
  }
}

@Composable
private fun CommentLongListScreen(
  title: String,
  configuration: CommentConfiguration,
  compactStyle: Boolean,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val dataSource = remember(compactStyle) { FeatureRichLongListDataSource(compactStyle) }
  var controllerRef by remember { mutableStateOf<CommentListController?>(null) }

  val listener = remember {
    object : CommentListListener {
      override fun onToggleLike(
        item: CommentItem,
        previousIsLiked: Boolean,
        previousLikeCount: Int,
        previousLikeCountText: String?,
      ) {
        scope.launch {
          delay(350)
          if (item.id == FeatureRichLongListDataSource.ROLLBACK_LIKE_ID) {
            controllerRef?.rollbackLike(
              id = item.id,
              previousIsLiked = previousIsLiked,
              previousLikeCount = previousLikeCount,
              previousLikeCountText = previousLikeCountText,
            )
            Toast.makeText(context, "Like failed — rolled back", Toast.LENGTH_SHORT).show()
          } else {
            controllerRef?.acknowledgeLike(item.id)
          }
        }
      }

      override fun onTapAvatar(item: CommentItem) {
        Toast.makeText(context, "Avatar: ${item.authorName}", Toast.LENGTH_SHORT).show()
      }

      override fun onTapAuthor(item: CommentItem) {
        Toast.makeText(context, "Author: ${item.authorName}", Toast.LENGTH_SHORT).show()
      }

      override fun onSelectMore(action: CommentMoreAction, item: CommentItem) {
        Toast.makeText(context, action.name, Toast.LENGTH_SHORT).show()
      }
    }
  }

  val listController = rememberCommentListController(
    configuration = configuration,
    dataSource = dataSource,
    listener = listener,
  )
  LaunchedEffect(listController) {
    controllerRef = listController
  }

  Scaffold(
    topBar = { SampleTopBar(title = title, onBack = onBack) },
  ) { padding ->
    CommentListHost(
      controller = listController,
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    )
  }
}

/**
 * Long-list mock: featured interaction rows up front, then scroll-stress pages.
 */
private class FeatureRichLongListDataSource(
  private val compactStyle: Boolean,
) : CommentListDataSource {
  private var nextPageIndex = 0
  private val pageSize = 12
  private val allTopLevel: List<CommentItem> = buildList {
    addAll(featureRows(compactStyle))
    addAll(makeLongScrollList(count = 80, compactStyle = compactStyle))
  }
  private val repliesByParent = featureReplies()

  override suspend fun loadInitial(): CommentListPage {
    delay(400)
    nextPageIndex = 1
    val page = allTopLevel.take(pageSize)
    return CommentListPage(items = page, hasMore = allTopLevel.size > page.size)
  }

  override suspend fun loadMore(): CommentListPage {
    delay(350)
    val start = pageSize * nextPageIndex
    if (start >= allTopLevel.size) {
      return CommentListPage(items = emptyList(), hasMore = false)
    }
    val end = minOf(allTopLevel.size, start + pageSize)
    nextPageIndex += 1
    return CommentListPage(
      items = allTopLevel.subList(start, end),
      hasMore = end < allTopLevel.size,
    )
  }

  override suspend fun loadReplies(parentId: String): List<CommentItem> {
    delay(350)
    return repliesByParent[parentId].orEmpty()
  }

  override suspend fun submit(request: CommentSubmitRequest): CommentItem {
    delay(400)
    val parentId = request.replyToCommentId
    val parent = parentId?.let { id ->
      allTopLevel.find { it.id == id }
        ?: repliesByParent.values.flatten().find { it.id == id }
    }
    val replyTo = parent?.let { CommentReplyTarget(id = it.id, displayName = it.authorName) }
    return CommentItem(
      id = "local-${UUID.randomUUID()}",
      authorName = "You",
      body = request.text,
      timestampText = "Just now",
      parentId = parentId,
      replyTo = replyTo,
      depth = if (parent != null) parent.depth + 1 else 0,
      isOwnedByCurrentUser = true,
      isDeletable = true,
    )
  }

  companion object {
    const val ROLLBACK_LIKE_ID = "feat.rollback"
  }
}

private fun featureRows(compactStyle: Boolean): List<CommentItem> {
  val longBody = "This body is intentionally long so expandable text can be exercised. ".repeat(8)
  return listOf(
    CommentItem(
      id = "feat.alex",
      authorName = "Alex Chen",
      avatarUrl = remoteAvatar(11),
      body = "Great write-up. Expand replies, like, reply, and open More on owned rows below.",
      timestampText = if (compactStyle) "2h ago" else "2h",
      likeCount = 12,
      replyCount = 3,
      isVerified = true,
    ),
    CommentItem(
      id = FeatureRichLongListDataSource.ROLLBACK_LIKE_ID,
      authorName = "Blair Rollback",
      avatarUrl = remoteAvatar(22),
      body = "Tapping like on this row simulates a failed persist (rollback).",
      timestampText = if (compactStyle) "1h ago" else "1h",
      likeCount = 3,
      replyCount = 0,
    ),
    CommentItem(
      id = "feat.owned",
      authorName = "You",
      avatarUrl = remoteAvatar(33),
      body = longBody,
      timestampText = if (compactStyle) "40m ago" else "40m",
      likeCount = 1,
      replyCount = 1,
      isOwnedByCurrentUser = true,
      isDeletable = true,
    ),
    CommentItem(
      id = "feat.taylor",
      authorName = "Taylor Kim",
      avatarUrl = remoteAvatar(44),
      body = "Could we also cover keyboard avoidance with the composer?",
      timestampText = if (compactStyle) "1d ago" else "1d",
      likeCount = 7,
      replyCount = 2,
      isVerified = true,
    ),
  )
}

private fun featureReplies(): Map<String, List<CommentItem>> = mapOf(
  "feat.alex" to listOf(
    CommentItem(
      id = "feat.alex.r1",
      authorName = "Riley Ng",
      avatarUrl = remoteAvatar(71),
      body = "Agreed — especially for news and social feeds.",
      timestampText = "1h",
      likeCount = 2,
      parentId = "feat.alex",
      replyTo = CommentReplyTarget("feat.alex", "Alex Chen"),
      depth = 1,
    ),
    CommentItem(
      id = "feat.alex.r2",
      authorName = "Alex Chen",
      avatarUrl = remoteAvatar(11),
      body = "Exactly. Nested trees get expensive fast.",
      timestampText = "50m",
      likeCount = 5,
      isLiked = true,
      parentId = "feat.alex",
      replyTo = CommentReplyTarget("feat.alex.r1", "Riley Ng"),
      depth = 1,
      isVerified = true,
      isOwnedByCurrentUser = true,
      isDeletable = true,
    ),
    CommentItem(
      id = "feat.alex.r3",
      authorName = "Jamie Ortiz",
      avatarUrl = remoteAvatar(72),
      body = "Thanks for the pointer to CommentKit.",
      timestampText = "40m",
      parentId = "feat.alex",
      replyTo = CommentReplyTarget("feat.alex", "Alex Chen"),
      depth = 1,
    ),
  ),
  "feat.owned" to listOf(
    CommentItem(
      id = "feat.owned.r1",
      authorName = "Pat Singh",
      avatarUrl = remoteAvatar(73),
      body = "Expandable body works well here.",
      timestampText = "30m",
      parentId = "feat.owned",
      replyTo = CommentReplyTarget("feat.owned", "You"),
      depth = 1,
    ),
  ),
  "feat.taylor" to listOf(
    CommentItem(
      id = "feat.taylor.r1",
      authorName = "Dev Support",
      avatarUrl = remoteAvatar(74),
      body = "Composer pins above the list on Android as well.",
      timestampText = "1d",
      parentId = "feat.taylor",
      replyTo = CommentReplyTarget("feat.taylor", "Taylor Kim"),
      depth = 1,
      isVerified = true,
    ),
    CommentItem(
      id = "feat.taylor.r2",
      authorName = "Taylor Kim",
      avatarUrl = remoteAvatar(44),
      body = "Perfect, thanks!",
      timestampText = "1d",
      parentId = "feat.taylor",
      replyTo = CommentReplyTarget("feat.taylor.r1", "Dev Support"),
      depth = 1,
    ),
  ),
)

private fun makeLongScrollList(
  count: Int,
  compactStyle: Boolean,
): List<CommentItem> {
  val names = listOf(
    "Alex Chen", "Jordan Lee", "Sam Rivera", "Taylor Kim", "Morgan Blake",
    "Casey Quinn", "Ming Xiao", "Travel Notes", "Local Guide", "Riley Ng",
  )
  val shortBodies = listOf(
    "Short note.",
    "Looks good to me.",
    "Thanks for sharing.",
    "Agreed.",
    "Nice catch.",
  )
  val mediumBodies = listOf(
    "The flat replyTo model feels closer to how most content apps ship comments.",
    "Could we also cover keyboard avoidance with the composer?",
    "Went last weekend. Queue was long but worth it.",
    "Try the slow-cooked beef set. Ask for less salt.",
  )
  val longFragment =
    "This body is intentionally long so expandable text and scroll cost can be exercised. "
  return List(count) { index ->
    val id = if (compactStyle) "long.sv.$index" else "long.c.$index"
    val likes = (index * 17) % 1400
    val likeText = if (compactStyle && likes >= 1000) {
      String.format(Locale.US, "%.1fk", likes / 1000.0)
    } else {
      null
    }
    val body = when (index % 5) {
      0 -> shortBodies[index % shortBodies.size]
      1, 2 -> mediumBodies[index % mediumBodies.size]
      else -> longFragment.repeat(2 + (index % 4))
    }
    CommentItem(
      id = id,
      authorName = names[index % names.size],
      avatarUrl = remoteAvatar(10 + (index % 90)),
      body = body,
      timestampText = if (compactStyle) {
        "${1 + index % 20}h ago"
      } else {
        "${1 + index % 48}h"
      },
      likeCount = likes,
      likeCountText = likeText,
      isLiked = index % 7 == 0,
      // Expand/replies are covered by the featured rows above; keep scroll rows leaf-only.
      replyCount = 0,
      isVerified = index % 9 == 0,
      isOwnedByCurrentUser = index % 23 == 0,
      isDeletable = index % 23 == 0,
    )
  }
}

private fun remoteAvatar(id: Int): String =
  "https://picsum.photos/id/$id/80/80"
