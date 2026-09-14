package com.fk.business.comment

/**
 * Flat-list mutation helpers for reply expand / submit / delete.
 *
 * Conceptually aligned with iOS `FKCommentListMutation`.
 */
object CommentListMutation {
  fun insertingReplies(
    replies: List<CommentItem>,
    parentId: String,
    into: List<CommentItem>,
    maxCount: Int,
  ): List<CommentItem> {
    if (into.none { it.id == parentId }) return into
    val collapsed = collapsingReplies(parentId = parentId, inItems = into).toMutableList()
    val parentIndex = collapsed.indexOfFirst { it.id == parentId }
    if (parentIndex < 0) return into
    val parentDepth = collapsed[parentIndex].depth
    val capped = replies.take(maxCount.coerceAtLeast(0)).map { reply ->
      val nextDepth = maxOf(parentDepth + 1, if (reply.depth == 0) parentDepth + 1 else reply.depth)
      reply.copy(parentId = parentId, depth = nextDepth)
    }
    collapsed.addAll(parentIndex + 1, capped)
    collapsed[parentIndex] = collapsed[parentIndex].copy(areRepliesExpanded = true)
    return collapsed
  }

  fun collapsingReplies(parentId: String, inItems: List<CommentItem>): List<CommentItem> {
    val parentIndex = inItems.indexOfFirst { it.id == parentId }
    if (parentIndex < 0) return inItems
    val result = inItems.toMutableList()
    val end = endIndexOfSubtree(rootedAt = parentIndex, inItems = result)
    if (end > parentIndex + 1) {
      result.subList(parentIndex + 1, end).clear()
    }
    result[parentIndex] = result[parentIndex].copy(areRepliesExpanded = false)
    return result
  }

  fun insertingSubmitted(item: CommentItem, into: List<CommentItem>): List<CommentItem> {
    if (into.any { it.id == item.id }) return replacing(item, into)
    val result = into.toMutableList()
    val parentId = item.parentId ?: item.replyTo?.id
    if (parentId != null) {
      val parentIndex = result.indexOfFirst { it.id == parentId }
      if (parentIndex >= 0) {
        val parentDepth = result[parentIndex].depth
        val copy = item.copy(
          parentId = parentId,
          depth = maxOf(parentDepth + 1, item.depth),
        )
        val insertAt = endIndexOfSubtree(rootedAt = parentIndex, inItems = result)
        result.add(insertAt, copy)
        val parent = result[parentIndex]
        result[parentIndex] = parent.copy(
          replyCount = parent.replyCount + 1,
          areRepliesExpanded = true,
        )
        return result
      }
    }
    result.add(item.copy(depth = 0, parentId = null))
    return result
  }

  fun replacing(item: CommentItem, inItems: List<CommentItem>): List<CommentItem> =
    inItems.map { if (it.id == item.id) item else it }

  fun removing(id: String, from: List<CommentItem>): List<CommentItem> {
    val removedIndex = from.indexOfFirst { it.id == id }
    if (removedIndex < 0) return from
    val removed = from[removedIndex]
    val result = from.toMutableList()
    val end = endIndexOfSubtree(rootedAt = removedIndex, inItems = result)
    result.subList(removedIndex, end).clear()
    val parentId = removed.parentId
    if (parentId != null) {
      val parentIndex = result.indexOfFirst { it.id == parentId }
      if (parentIndex >= 0) {
        val parent = result[parentIndex]
        val stillHasChildren = result.any { it.parentId == parentId }
        result[parentIndex] = parent.copy(
          replyCount = (parent.replyCount - 1).coerceAtLeast(0),
          areRepliesExpanded = if (stillHasChildren) parent.areRepliesExpanded else false,
        )
      }
    }
    return result
  }

  fun appendingUnique(pageItems: List<CommentItem>, onto: List<CommentItem>): List<CommentItem> {
    val existing = onto.mapTo(HashSet()) { it.id }
    return onto + pageItems.filter { it.id !in existing }
  }

  fun endIndexOfSubtree(rootedAt: Int, inItems: List<CommentItem>): Int {
    if (rootedAt !in inItems.indices) return rootedAt
    val subtreeIds = mutableSetOf(inItems[rootedAt].id)
    var index = rootedAt + 1
    while (index < inItems.size) {
      val parentId = inItems[index].parentId ?: break
      if (parentId !in subtreeIds) break
      subtreeIds.add(inItems[index].id)
      index++
    }
    return index
  }
}

/**
 * Optimistic like toggle / rollback helpers.
 *
 * Conceptually aligned with iOS `FKCommentLikeOptimisticController`.
 */
object CommentLikeOptimistic {
  fun toggled(item: CommentItem): CommentItem =
    if (item.isLiked) {
      item.copy(
        isLiked = false,
        likeCount = (item.likeCount - 1).coerceAtLeast(0),
        likeCountText = null,
      )
    } else {
      item.copy(
        isLiked = true,
        likeCount = item.likeCount + 1,
        likeCountText = null,
      )
    }

  fun rolledBack(
    item: CommentItem,
    previousIsLiked: Boolean,
    previousLikeCount: Int,
    previousLikeCountText: String? = null,
  ): CommentItem =
    item.copy(
      isLiked = previousIsLiked,
      likeCount = previousLikeCount.coerceAtLeast(0),
      likeCountText = previousLikeCountText,
    )
}
