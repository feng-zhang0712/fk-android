package com.fk.ui.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import kotlinx.coroutines.launch

/**
 * Product bottom sheet over Material3 [ModalBottomSheet].
 *
 * Maps [SheetConfiguration.detents] onto Material's partial/expanded anchors and
 * applies height guidance for fraction / fit-content modes.
 *
 * Note: Material3 always routes scrim taps and swipe-to-dismiss through
 * [onDismissRequest]. [SheetConfiguration.dismissOnClickOutside] is honored by
 * [FkCenterSheet]; for bottom sheets the host should treat [onDismissRequest] as
 * the single dismiss signal (see README).
 *
 * Conceptually aligned with iOS bottom-sheet mode of `FKSheetPresentationController`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FkBottomSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  configuration: SheetConfiguration = SheetConfiguration.BottomSheetDefault,
  sheetState: SheetState = rememberFkBottomSheetState(configuration),
  content: @Composable ColumnScope.() -> Unit,
) {
  require(configuration.presentation == SheetPresentation.BottomSheet) {
    "FkBottomSheet requires SheetPresentation.BottomSheet"
  }

  val metrics = fkMetrics()
  val skipPartial = shouldSkipPartiallyExpanded(configuration.detents)

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    sheetState = sheetState,
    shape = metrics.shapeLarge,
    containerColor = fkColor(FkColorRole.SurfaceElevated),
    contentColor = fkColor(FkColorRole.OnSurface),
    scrimColor = scrimColor(configuration.backdrop),
    dragHandle = if (configuration.showDragHandle) {
      { BottomSheetDefaults.DragHandle() }
    } else {
      null
    },
    properties = ModalBottomSheetProperties(
      shouldDismissOnBackPress = configuration.dismissOnBack,
    ),
  ) {
    Column(
      modifier = bottomSheetContentModifier(configuration, skipPartial)
        .fillMaxWidth()
        .imePadding(),
      content = content,
    )
  }
}

/**
 * Remembers a [SheetState] mapped from [configuration] detents.
 *
 * Prefer creating this **inside** the `if (visible)` branch that hosts [FkBottomSheet],
 * so a dismissed (Hidden) state is not reused on the next open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFkBottomSheetState(
  configuration: SheetConfiguration = SheetConfiguration.BottomSheetDefault,
  confirmValueChange: (SheetValue) -> Boolean = { true },
): SheetState {
  val skipPartial = shouldSkipPartiallyExpanded(configuration.detents)
  return key(skipPartial, configuration.detents) {
    rememberModalBottomSheetState(
      skipPartiallyExpanded = skipPartial,
      confirmValueChange = confirmValueChange,
    )
  }
}

/**
 * Imperative expand / partial-expand / hide helpers for a [SheetState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSheetController(
  sheetState: SheetState,
  skipPartiallyExpanded: Boolean = false,
): SheetController {
  val scope = rememberCoroutineScope()
  return remember(sheetState, skipPartiallyExpanded) {
    SheetController(
      expand = { scope.launch { sheetState.expand() } },
      partialExpand = {
        scope.launch {
          if (skipPartiallyExpanded) {
            sheetState.expand()
          } else {
            sheetState.partialExpand()
          }
        }
      },
      hide = { onHidden ->
        scope.launch {
          sheetState.hide()
          onHidden()
        }
      },
    )
  }
}

/**
 * Imperative helpers for [FkBottomSheet] state.
 */
class SheetController(
  val expand: () -> Unit,
  val partialExpand: () -> Unit,
  val hide: (onHidden: () -> Unit) -> Unit,
)

/**
 * Whether Material should skip the partially-expanded anchor for [detents].
 */
fun shouldSkipPartiallyExpanded(detents: List<SheetDetent>): Boolean {
  if (detents.size <= 1) return true
  val hasMediumOrFit = detents.any {
    it is SheetDetent.Medium ||
      it is SheetDetent.FitContent ||
      (it is SheetDetent.Fraction && it.fraction <= 0.6f)
  }
  val hasTall = detents.any {
    it is SheetDetent.Large ||
      it is SheetDetent.Full ||
      (it is SheetDetent.Fraction && it.fraction > 0.6f)
  }
  return !(hasMediumOrFit && hasTall)
}

@Composable
private fun bottomSheetContentModifier(
  configuration: SheetConfiguration,
  skipPartial: Boolean,
): Modifier {
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val primary = configuration.detents.first()
  return when {
    !skipPartial -> Modifier.fillMaxHeight(0.92f)
    primary is SheetDetent.FitContent ->
      Modifier
        .wrapContentHeight()
        .heightIn(max = screenHeight * 0.9f)
    primary is SheetDetent.Fraction -> Modifier.fillMaxHeight(primary.fraction)
    primary is SheetDetent.Medium -> Modifier.fillMaxHeight(0.5f)
    primary is SheetDetent.Large -> Modifier.fillMaxHeight(0.92f)
    primary is SheetDetent.Full -> Modifier.fillMaxHeight(1f)
    else -> Modifier.wrapContentHeight()
  }
}

internal fun scrimColor(backdrop: SheetBackdrop): Color =
  when (backdrop) {
    SheetBackdrop.None -> Color.Transparent
    is SheetBackdrop.Dim -> Color.Black.copy(alpha = backdrop.alpha)
  }
