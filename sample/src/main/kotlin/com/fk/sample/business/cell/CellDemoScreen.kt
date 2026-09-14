package com.fk.sample.business.cell

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.fk.business.cell.CellChromeStyle
import com.fk.business.cell.CellKit
import com.fk.business.cell.CellPresence
import com.fk.business.cell.CellStatusPill
import com.fk.business.cell.CellTag
import com.fk.business.cell.InlineToggleItem
import com.fk.business.cell.InlineToggleRow
import com.fk.business.cell.NotificationListItem
import com.fk.business.cell.NotificationListRow
import com.fk.business.cell.OrderListItem
import com.fk.business.cell.OrderListRow
import com.fk.business.cell.SearchResultItem
import com.fk.business.cell.SearchResultRow
import com.fk.business.cell.UserListItem
import com.fk.business.cell.UserListRow
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkSpacingToken
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Demo for Phase F3 selective cell rows.
 */
@Composable
fun CellDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val metrics = fkMetrics()
  var pushEnabled by remember { mutableStateOf(true) }
  var marketingEnabled by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      SampleTopBar(title = "Cell", onBack = onBack)
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    ) {
      item {
        SectionHeader("CellKit ${CellKit.VERSION} — selective rows")
      }
      item {
        SectionHeader("User")
      }
      item {
        UserListRow(
          item = UserListItem(
            id = "u1",
            displayName = "Ada Lovelace",
            subtitle = "Online · Engineering",
            presence = CellPresence.Online,
            unreadCount = 3,
            roleTag = CellTag("Admin", CellChromeStyle.Info),
            timestampText = "2m",
            isVerified = true,
          ),
          onClick = {
            Toast.makeText(context, "Open Ada", Toast.LENGTH_SHORT).show()
          },
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }
      item {
        UserListRow(
          item = UserListItem(
            id = "u2",
            displayName = "Grace Hopper",
            subtitle = "Away",
            presence = CellPresence.Away,
            timestampText = "1h",
          ),
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }

      item { SectionHeader("Notification") }
      item {
        NotificationListRow(
          item = NotificationListItem(
            id = "n1",
            title = "New comment on your post",
            summary = "Blair replied: Looks great — ship it.",
            timestampText = "Just now",
            isUnread = true,
          ),
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }
      item {
        NotificationListRow(
          item = NotificationListItem(
            id = "n2",
            title = "Weekly digest is ready",
            summary = "12 unread highlights from this week.",
            timestampText = "Yesterday",
            isUnread = false,
          ),
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }

      item { SectionHeader("Search") }
      item {
        SearchResultRow(
          item = SearchResultItem.highlight(
            id = "s1",
            title = "Jetpack Compose handbook",
            query = "Compose",
            breadcrumbText = "Docs › UI › Compose",
            categoryTagTitle = "Guide",
          ),
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }

      item { SectionHeader("Order") }
      item {
        OrderListRow(
          item = OrderListItem(
            id = "o1",
            title = "FK Pro Annual",
            subtitle = "Paid · ¥998",
            displayOrderNumber = "ORD-2048",
            fullOrderNumber = "ORD-2048-9F3A",
            statusPill = CellStatusPill(
              title = "Shipped",
              style = CellChromeStyle.Success,
              showsDot = true,
            ),
          ),
          onCopyOrderNumber = { number ->
            clipboard.setText(AnnotatedString(number))
            Toast.makeText(context, "Copied $number", Toast.LENGTH_SHORT).show()
          },
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }
      item {
        OrderListRow(
          item = OrderListItem(
            id = "o2",
            title = "Support ticket #441",
            subtitle = "Awaiting reply",
            displayOrderNumber = "T-441",
            statusPill = CellStatusPill("Open", CellChromeStyle.Warning, showsDot = true),
            showsCopyChip = false,
          ),
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }

      item { SectionHeader("Inline toggle") }
      item {
        InlineToggleRow(
          item = InlineToggleItem(
            id = "t1",
            title = "Push notifications",
            subtitle = "Mentions, replies, and system alerts",
            isOn = pushEnabled,
          ),
          onCheckedChange = { pushEnabled = it },
        )
      }
      item { HorizontalDivider(color = fkColor(FkColorRole.Outline)) }
      item {
        InlineToggleRow(
          item = InlineToggleItem(
            id = "t2",
            title = "Marketing emails",
            subtitle = "Disabled by policy",
            isOn = marketingEnabled,
            isEnabled = false,
          ),
          onCheckedChange = { marketingEnabled = it },
        )
      }

      item {
        Text(
          text = "Skipped: comment thread, feed video, cart, grids, ListKit chrome.",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
          modifier = Modifier.padding(metrics.spacing(FkSpacingToken.L)),
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  val metrics = fkMetrics()
  Text(
    text = title,
    style = fkTextStyle(FkTextStyle.Footnote).copy(fontWeight = FontWeight.SemiBold),
    color = fkColor(FkColorRole.OnSurfaceSecondary),
    modifier = Modifier.padding(
      start = metrics.spacingM,
      end = metrics.spacingM,
      top = metrics.spacingM,
      bottom = metrics.spacingXs,
    ),
  )
}
