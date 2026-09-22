package com.fk.ui.callout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import java.util.UUID

/**
 * Tracks an on-screen anchor for callout presentation.
 *
 * Attach with [Modifier.calloutAnchor], then pass the state into show APIs.
 */
@Stable
class CalloutAnchorState(
  val id: String = UUID.randomUUID().toString(),
) {
  var boundsInWindow: Rect by mutableStateOf(Rect.Zero)
    internal set

  var isAttached: Boolean by mutableStateOf(false)
    internal set

  internal fun update(coordinates: LayoutCoordinates) {
    if (!coordinates.isAttached) {
      isAttached = false
      boundsInWindow = Rect.Zero
      return
    }
    val position = coordinates.positionInWindow()
    val size = coordinates.size
    boundsInWindow = Rect(
      left = position.x,
      top = position.y,
      right = position.x + size.width,
      bottom = position.y + size.height,
    )
    isAttached = true
  }
}

@Composable
fun rememberCalloutAnchorState(
  id: String = remember { UUID.randomUUID().toString() },
): CalloutAnchorState = remember(id) { CalloutAnchorState(id) }

/** Registers this layout node as a callout anchor. */
fun Modifier.calloutAnchor(state: CalloutAnchorState): Modifier =
  this.onGloballyPositioned { coordinates ->
    state.update(coordinates)
  }
