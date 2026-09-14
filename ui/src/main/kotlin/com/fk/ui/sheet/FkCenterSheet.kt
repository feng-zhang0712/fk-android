package com.fk.ui.sheet

import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics

/**
 * Centered floating card sheet (dialog-style).
 *
 * Conceptually aligned with iOS center presentation of `FKSheetPresentationController`.
 *
 * Dim amount is applied on the dialog window (avoids stacking a second Compose scrim
 * on top of the platform dialog dim).
 */
@Composable
fun FkCenterSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  configuration: SheetConfiguration = SheetConfiguration.CenterCard,
  content: @Composable BoxScope.() -> Unit,
) {
  require(configuration.presentation == SheetPresentation.Center) {
    "FkCenterSheet requires SheetPresentation.Center"
  }

  val metrics = fkMetrics()
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      dismissOnBackPress = configuration.dismissOnBack,
      dismissOnClickOutside = configuration.dismissOnClickOutside,
      usePlatformDefaultWidth = false,
    ),
  ) {
    val dimAmount = when (val backdrop = configuration.backdrop) {
      SheetBackdrop.None -> 0f
      is SheetBackdrop.Dim -> backdrop.alpha
    }
    val view = LocalView.current
    SideEffect {
      val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
      window.setDimAmount(dimAmount)
      if (dimAmount <= 0f) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
      } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
      }
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = modifier
          .padding(metrics.spacingL)
          .widthIn(max = 460.dp)
          .wrapContentHeight()
          .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = { /* consume so outside-dismiss does not fire from card taps */ },
          ),
        shape = metrics.shapeLarge,
        color = fkColor(FkColorRole.SurfaceElevated),
        contentColor = fkColor(FkColorRole.OnSurface),
        shadowElevation = 6.dp,
      ) {
        Box(content = content)
      }
    }
  }
}
