package com.fk.sample.business.comment

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Full demo for Phase F1 comment (list + composer, mock data source).
 */
@Composable
fun CommentDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val dataSource = remember { MockCommentDataSource() }
  var controller by remember { mutableStateOf<CommentListController?>(null) }
  val listener = remember {
    object : CommentListListener {
      override fun onToggleLike(
        item: CommentItem,
        previousIsLiked: Boolean,
        previousLikeCount: Int,
        previousLikeCountText: String?,
      ) {
        scope.launch {
          delay(400)
          // Simulate persistence failure on Blair's row (id ends with "2").
          if (item.id.endsWith("2")) {
            controller?.rollbackLike(
              id = item.id,
              previousIsLiked = previousIsLiked,
              previousLikeCount = previousLikeCount,
              previousLikeCountText = previousLikeCountText,
            )
            Toast.makeText(context, "Like failed — rolled back", Toast.LENGTH_SHORT).show()
          } else {
            controller?.acknowledgeLike(item.id)
          }
        }
      }
    }
  }
  val listController = rememberCommentListController(
    configuration = CommentConfiguration(),
    dataSource = dataSource,
    listener = listener,
  )
  LaunchedEffect(listController) {
    controller = listController
  }

  Scaffold(
    topBar = {
      SampleTopBar(
        title = "Comment v${CommentKit.VERSION}",
        onBack = onBack,
      )
    },
  ) { padding ->
    CommentListHost(
      controller = listController,
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      onMore = { item ->
        clipboard.setText(AnnotatedString(item.body))
        listController.selectMore(CommentMoreAction.Copy, item)
        Toast.makeText(context, CommentMoreAction.Copy.name, Toast.LENGTH_SHORT).show()
        if (item.canDelete) {
          listController.selectMore(CommentMoreAction.Delete, item)
        }
      },
    )
  }
}

private class MockCommentDataSource : CommentListDataSource {
  private var page = 0

  private val seed = listOf(
    CommentItem(
      id = "c1",
      authorName = "Alex",
      body = "Love this feature — especially the optimistic like.",
      timestampText = "2h",
      likeCount = 12,
      replyCount = 2,
      isVerified = true,
    ),
    CommentItem(
      id = "c2",
      authorName = "Blair",
      body = "Tapping like on this row simulates a failed persist (rollback).",
      timestampText = "1h",
      likeCount = 3,
      replyCount = 0,
    ),
    CommentItem(
      id = "c3",
      authorName = "Casey",
      body = "You can reply to me — expand replies on Alex for nested rows.",
      timestampText = "40m",
      likeCount = 1,
      replyCount = 1,
      isOwnedByCurrentUser = true,
    ),
  )

  private val replies = mapOf(
    "c1" to listOf(
      CommentItem(
        id = "c1-r1",
        authorName = "Dana",
        body = "Agreed!",
        timestampText = "1h",
        parentId = "c1",
        replyTo = CommentReplyTarget("c1", "Alex"),
        depth = 1,
        likeCount = 2,
      ),
      CommentItem(
        id = "c1-r2",
        authorName = "Evan",
        body = "Same here.",
        timestampText = "50m",
        parentId = "c1",
        replyTo = CommentReplyTarget("c1", "Alex"),
        depth = 1,
      ),
    ),
    "c3" to listOf(
      CommentItem(
        id = "c3-r1",
        authorName = "Alex",
        body = "Thanks Casey!",
        timestampText = "30m",
        parentId = "c3",
        replyTo = CommentReplyTarget("c3", "Casey"),
        depth = 1,
      ),
    ),
  )

  override suspend fun loadInitial(): CommentListPage {
    delay(500)
    page = 1
    return CommentListPage(items = seed, hasMore = true)
  }

  override suspend fun loadMore(): CommentListPage {
    delay(500)
    page += 1
    val more = listOf(
      CommentItem(
        id = "c-page-$page",
        authorName = "Page$page",
        body = "Loaded from page $page (mock load-more).",
        timestampText = "now",
      ),
    )
    return CommentListPage(items = more, hasMore = page < 3)
  }

  override suspend fun loadReplies(parentId: String): List<CommentItem> {
    delay(400)
    return replies[parentId].orEmpty()
  }

  override suspend fun submit(request: CommentSubmitRequest): CommentItem {
    delay(450)
    val parentId = request.replyToCommentId
    val replyTo = parentId?.let { id ->
      val name = seed.find { it.id == id }?.authorName
        ?: replies.values.flatten().find { it.id == id }?.authorName
        ?: "User"
      CommentReplyTarget(id = id, displayName = name)
    }
    return CommentItem(
      id = "local-${UUID.randomUUID()}",
      authorName = "You",
      body = request.text,
      timestampText = "now",
      parentId = parentId,
      replyTo = replyTo,
      depth = if (parentId != null) 1 else 0,
      isOwnedByCurrentUser = true,
    )
  }
}
