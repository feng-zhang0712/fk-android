package com.fk.sample.ui.rating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.fk.business.R as BusinessR
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.rating.FkRating
import com.fk.ui.rating.FkRatingInteractive
import com.fk.ui.rating.FkRatingReadOnly
import com.fk.ui.rating.RatingAppearanceConfiguration
import com.fk.ui.rating.RatingConfiguration
import com.fk.ui.rating.RatingIconStyle
import com.fk.ui.rating.RatingInteractionConfiguration
import com.fk.ui.rating.RatingInteractionMode
import com.fk.ui.rating.RatingLabelConfiguration
import com.fk.ui.rating.RatingLabelPlacement
import com.fk.ui.rating.RatingLayoutConfiguration
import com.fk.ui.rating.RatingStep
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

private enum class RatingDemoDestination {
  Hub,
  Basics,
  Steps,
  Interaction,
  Layout,
  Appearance,
  Caption,
  RangeAccessibility,
  Playground,
}

/**
 * Full-scenario Rating demos with a flat hub entry list.
 */
@Composable
fun RatingDemoScreen(
  onBack: () -> Unit,
) {
  var destination by remember { mutableStateOf(RatingDemoDestination.Hub) }
  when (destination) {
    RatingDemoDestination.Hub -> RatingDemoHub(
      onBack = onBack,
      onOpen = { destination = it },
    )
    RatingDemoDestination.Basics -> RatingBasicsScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Steps -> RatingStepsScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Interaction -> RatingInteractionScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Layout -> RatingLayoutScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Appearance -> RatingAppearanceScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Caption -> RatingCaptionScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.RangeAccessibility -> RatingRangeAccessibilityScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
    RatingDemoDestination.Playground -> RatingPlaygroundScreen(
      onBack = { destination = RatingDemoDestination.Hub },
    )
  }
}

@Composable
private fun RatingDemoHub(
  onBack: () -> Unit,
  onOpen: (RatingDemoDestination) -> Unit,
) {
  val metrics = fkMetrics()
  val entries = listOf(
    Triple(
      "Basics",
      "FkRating / FkRatingReadOnly / FkRatingInteractive",
      RatingDemoDestination.Basics,
    ),
    Triple(
      "Steps",
      "Whole · Half · Custom snap increments",
      RatingDemoDestination.Steps,
    ),
    Triple(
      "Interaction",
      "Drag · Disabled · Haptics · RTL",
      RatingDemoDestination.Interaction,
    ),
    Triple(
      "Layout",
      "itemCount / itemSize / itemSpacing / contentPadding",
      RatingDemoDestination.Layout,
    ),
    Triple(
      "Appearance",
      "Colors · Formatter · Custom painters",
      RatingDemoDestination.Appearance,
    ),
    Triple(
      "Caption",
      "Trailing · Bottom · Prefix / Suffix · Custom",
      RatingDemoDestination.Caption,
    ),
    Triple(
      "Range & accessibility",
      "Min / Max · TalkBack strings",
      RatingDemoDestination.RangeAccessibility,
    ),
    Triple(
      "Playground",
      "Toggle major options on one interactive control",
      RatingDemoDestination.Playground,
    ),
  )
  Scaffold(
    topBar = { SampleTopBar(title = "Rating", onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      entries.forEachIndexed { index, (title, subtitle, dest) ->
        HubRow(
          title = title,
          subtitle = subtitle,
          onClick = { onOpen(dest) },
          showDivider = index < entries.lastIndex,
        )
      }
      Text(
        text = "Scenarios cover every public Rating API.",
        style = fkTextStyle(FkTextStyle.Caption1),
        color = fkColor(FkColorRole.OnSurfaceSecondary),
        modifier = Modifier.padding(
          horizontal = metrics.spacingM,
          vertical = metrics.spacingM,
        ),
      )
    }
  }
}

@Composable
private fun HubRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  showDivider: Boolean = true,
) {
  ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(subtitle) },
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  )
  if (showDivider) {
    HorizontalDivider(color = fkColor(FkColorRole.Outline))
  }
}

// region Basics

@Composable
private fun RatingBasicsScreen(onBack: () -> Unit) {
  var interactive by remember { mutableDoubleStateOf(3.5) }
  var full by remember { mutableDoubleStateOf(2.0) }
  DemoScaffold(title = "Basics", onBack = onBack) {
    Section("FkRatingReadOnly")
    Hint("Displays 4.5 with default half-step fill (no interaction).")
    FkRatingReadOnly(value = 4.5)
    ValueLine("value=4.5")

    Section("FkRatingInteractive (default half step)")
    Hint("Tap or drag to change; snaps to 0.5.")
    FkRatingInteractive(
      value = interactive,
      onValueChange = { interactive = it },
    )
    ValueLine("value=$interactive")

    Section("FkRating (full API, whole step)")
    FkRating(
      value = full,
      onValueChange = { full = it },
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Whole,
        ),
      ),
    )
    ValueLine("value=$full")
  }
}

// endregion

// region Steps

@Composable
private fun RatingStepsScreen(onBack: () -> Unit) {
  var whole by remember { mutableDoubleStateOf(3.0) }
  var half by remember { mutableDoubleStateOf(3.5) }
  var custom by remember { mutableDoubleStateOf(2.25) }
  DemoScaffold(title = "Steps", onBack = onBack) {
    Section("Whole")
    FkRatingInteractive(
      value = whole,
      onValueChange = { whole = it },
      step = RatingStep.Whole,
    )
    ValueLine("value=$whole · step=1.0")

    Section("Half")
    FkRatingInteractive(
      value = half,
      onValueChange = { half = it },
      step = RatingStep.Half,
    )
    ValueLine("value=$half · step=0.5")

    Section("Custom (0.25)")
    FkRating(
      value = custom,
      onValueChange = { custom = it },
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Custom(0.25),
        ),
      ),
    )
    ValueLine("value=$custom · step=0.25")

    Section("Read-only fractional display")
    Hint("Partial fills without interaction (3.5 / 4.25).")
    FkRatingReadOnly(value = 3.5, step = RatingStep.Half)
    FkRatingReadOnly(value = 4.25, step = RatingStep.Custom(0.25))
  }
}

// endregion

// region Interaction

@Composable
private fun RatingInteractionScreen(onBack: () -> Unit) {
  var dragOn by remember { mutableDoubleStateOf(2.5) }
  var tapOnly by remember { mutableDoubleStateOf(2.5) }
  var haptics by remember { mutableDoubleStateOf(3.0) }
  var rtl by remember { mutableDoubleStateOf(3.0) }
  DemoScaffold(title = "Interaction", onBack = onBack) {
    Section("Read-only mode")
    FkRating(
      value = 4.0,
      onValueChange = {},
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Half,
        ),
      ),
    )

    Section("Drag enabled")
    FkRatingInteractive(
      value = dragOn,
      onValueChange = { dragOn = it },
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
          allowsDragSelection = true,
        ),
      ),
    )
    ValueLine("value=$dragOn")

    Section("Tap only (drag disabled)")
    FkRating(
      value = tapOnly,
      onValueChange = { tapOnly = it },
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
          allowsDragSelection = false,
        ),
      ),
    )
    ValueLine("value=$tapOnly")

    Section("Disabled (alpha 0.45)")
    FkRating(
      value = 3.5,
      onValueChange = {},
      enabled = false,
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )

    Section("Disabled (custom alpha 0.25)")
    FkRating(
      value = 3.5,
      onValueChange = {},
      enabled = false,
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
          disabledAlpha = 0.25f,
        ),
      ),
    )

    Section("Haptics on value change")
    FkRating(
      value = haptics,
      onValueChange = { haptics = it },
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
          enableHaptics = true,
        ),
      ),
    )
    ValueLine("value=$haptics")

    Section("RTL layout direction")
    Hint("Pointer mapping mirrors under RTL.")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      FkRatingInteractive(
        value = rtl,
        onValueChange = { rtl = it },
      )
    }
    ValueLine("value=$rtl")
  }
}

// endregion

// region Layout

@Composable
private fun RatingLayoutScreen(onBack: () -> Unit) {
  var score by remember { mutableDoubleStateOf(3.0) }
  DemoScaffold(title = "Layout", onBack = onBack) {
    Section("itemCount = 3")
    FkRatingInteractive(
      value = score.coerceAtMost(3.0),
      onValueChange = { score = it },
      itemCount = 3,
    )

    Section("itemCount = 7")
    var seven by remember { mutableDoubleStateOf(4.0) }
    FkRatingInteractive(
      value = seven,
      onValueChange = { seven = it },
      itemCount = 7,
    )
    ValueLine("value=$seven")

    Section("Large items (40dp) + wide spacing (12dp)")
    var large by remember { mutableDoubleStateOf(2.5) }
    FkRating(
      value = large,
      onValueChange = { large = it },
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          itemSize = 40.dp,
          itemSpacing = 12.dp,
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )

    Section("Compact items (18dp) + tight spacing (2dp)")
    FkRatingReadOnly(
      value = 4.5,
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          itemSize = 18.dp,
          itemSpacing = 2.dp,
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Half,
        ),
      ),
    )

    Section("contentPadding = 12dp")
    FkRatingInteractive(
      value = 3.5,
      onValueChange = {},
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(contentPadding = 12.dp),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Half,
        ),
      ),
    )
  }
}

// endregion

// region Appearance

@Composable
private fun RatingAppearanceScreen(onBack: () -> Unit) {
  var themed by remember { mutableDoubleStateOf(3.5) }
  var hearts by remember { mutableDoubleStateOf(4.0) }
  DemoScaffold(title = "Appearance", onBack = onBack) {
    Section("Custom colors")
    FkRating(
      value = themed,
      onValueChange = { themed = it },
      configuration = RatingConfiguration(
        appearance = RatingAppearanceConfiguration(
          emptyColor = Color(0xFFBDBDBD),
          filledColor = Color(0xFFE91E63),
          labelColor = Color(0xFFAD1457),
        ),
        layout = RatingLayoutConfiguration(
          labelPlacement = RatingLabelPlacement.Trailing,
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )
    ValueLine("value=$themed")

    Section("Custom valueFormatter")
    FkRatingReadOnly(
      value = 4.5,
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          labelPlacement = RatingLabelPlacement.Trailing,
        ),
        appearance = RatingAppearanceConfiguration(
          valueFormatter = { v -> String.format("%.2f★", v) },
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Half,
        ),
      ),
    )

    Section("Custom painters (heart outline / fill)")
    val emptyHeart = painterResource(BusinessR.drawable.fk_ic_heart_outline)
    val filledHeart = painterResource(BusinessR.drawable.fk_ic_heart_fill)
    FkRating(
      value = hearts,
      onValueChange = { hearts = it },
      configuration = RatingConfiguration(
        appearance = RatingAppearanceConfiguration(
          iconStyle = RatingIconStyle.Painters(
            empty = emptyHeart,
            filled = filledHeart,
          ),
          emptyColor = Color(0xFFC7C7CC),
          filledColor = Color(0xFFFF2D55),
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )
    ValueLine("value=$hearts")
  }
}

// endregion

// region Caption

@Composable
private fun RatingCaptionScreen(onBack: () -> Unit) {
  var trailing by remember { mutableDoubleStateOf(4.0) }
  var bottom by remember { mutableDoubleStateOf(3.5) }
  DemoScaffold(title = "Caption", onBack = onBack) {
    Section("None (default)")
    FkRatingReadOnly(value = 4.5)

    Section("Trailing + default numeric")
    FkRating(
      value = trailing,
      onValueChange = { trailing = it },
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          labelPlacement = RatingLabelPlacement.Trailing,
          labelSpacing = 8.dp,
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )
    ValueLine("value=$trailing")

    Section("Bottom + prefix / suffix")
    FkRating(
      value = bottom,
      onValueChange = { bottom = it },
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          labelPlacement = RatingLabelPlacement.Bottom,
          labelSpacing = 4.dp,
        ),
        label = RatingLabelConfiguration(
          valuePrefix = "Score ",
          valueSuffix = " / 5",
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )
    ValueLine("value=$bottom")

    Section("Custom caption text")
    FkRatingReadOnly(
      value = 5.0,
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          labelPlacement = RatingLabelPlacement.Trailing,
        ),
        label = RatingLabelConfiguration(customText = "Excellent"),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Whole,
        ),
      ),
    )
  }
}

// endregion

// region Range & accessibility

@Composable
private fun RatingRangeAccessibilityScreen(onBack: () -> Unit) {
  var ranged by remember { mutableDoubleStateOf(2.0) }
  DemoScaffold(title = "Range & a11y", onBack = onBack) {
    Section("minimumValue = 1 · maximumValue = 5")
    Hint("Cannot snap below 1.0.")
    FkRating(
      value = ranged,
      onValueChange = { ranged = it },
      minimumValue = 1.0,
      maximumValue = 5.0,
      configuration = RatingConfiguration(
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = RatingStep.Half,
        ),
      ),
    )
    ValueLine("value=$ranged")

    Section("Custom accessibility copy")
    Hint("Inspect with TalkBack: contentDescription + stateDescription format.")
    FkRatingInteractive(
      value = 3.5,
      onValueChange = {},
      configuration = RatingConfiguration(
        contentDescription = "Product rating",
        accessibilityValueFormat = "%1\$s out of %2\$s stars",
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.ReadOnly,
          step = RatingStep.Half,
        ),
      ),
    )
  }
}

// endregion

// region Playground

@Composable
private fun RatingPlaygroundScreen(onBack: () -> Unit) {
  var value by remember { mutableDoubleStateOf(3.5) }
  var halfStep by remember { mutableStateOf(true) }
  var drag by remember { mutableStateOf(true) }
  var haptics by remember { mutableStateOf(false) }
  var enabled by remember { mutableStateOf(true) }
  var showCaption by remember { mutableStateOf(true) }
  var bottomCaption by remember { mutableStateOf(false) }
  var useHearts by remember { mutableStateOf(false) }
  var itemCountFive by remember { mutableStateOf(true) }

  val emptyHeart = painterResource(BusinessR.drawable.fk_ic_heart_outline)
  val filledHeart = painterResource(BusinessR.drawable.fk_ic_heart_fill)
  val itemCount = if (itemCountFive) 5 else 3
  val clamped = value.coerceIn(0.0, itemCount.toDouble())

  DemoScaffold(title = "Playground", onBack = onBack) {
    FkRating(
      value = clamped,
      onValueChange = { value = it },
      enabled = enabled,
      maximumValue = itemCount.toDouble(),
      configuration = RatingConfiguration(
        layout = RatingLayoutConfiguration(
          itemCount = itemCount,
          itemSize = 32.dp,
          labelPlacement = when {
            !showCaption -> RatingLabelPlacement.None
            bottomCaption -> RatingLabelPlacement.Bottom
            else -> RatingLabelPlacement.Trailing
          },
        ),
        appearance = RatingAppearanceConfiguration(
          iconStyle = if (useHearts) {
            RatingIconStyle.Painters(emptyHeart, filledHeart)
          } else {
            RatingIconStyle.Star
          },
          filledColor = if (useHearts) Color(0xFFFF2D55) else Color(0xFFFFCC00),
        ),
        interaction = RatingInteractionConfiguration(
          mode = RatingInteractionMode.Interactive,
          step = if (halfStep) RatingStep.Half else RatingStep.Whole,
          allowsDragSelection = drag,
          enableHaptics = haptics,
        ),
        label = RatingLabelConfiguration(valueSuffix = " pts"),
        contentDescription = "Playground rating",
      ),
    )
    ValueLine("value=$clamped")

    HorizontalDivider(color = fkColor(FkColorRole.Outline))
    ToggleRow("Half step", halfStep) { halfStep = it }
    ToggleRow("Allow drag", drag) { drag = it }
    ToggleRow("Haptics", haptics) { haptics = it }
    ToggleRow("Enabled", enabled) { enabled = it }
    ToggleRow("Show caption", showCaption) { showCaption = it }
    ToggleRow("Caption at bottom", bottomCaption) { bottomCaption = it }
    ToggleRow("Heart painters", useHearts) { useHearts = it }
    ToggleRow("5 items (off = 3)", itemCountFive) {
      itemCountFive = it
      value = value.coerceIn(0.0, if (it) 5.0 else 3.0)
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      FilterChip(
        selected = value == 0.0,
        onClick = { value = 0.0 },
        label = { Text("0") },
      )
      FilterChip(
        selected = value == 2.5,
        onClick = { value = 2.5 },
        label = { Text("2.5") },
      )
      FilterChip(
        selected = value == itemCount.toDouble(),
        onClick = { value = itemCount.toDouble() },
        label = { Text("Max") },
      )
    }
  }
}

// endregion

// region Shared chrome

@Composable
private fun DemoScaffold(
  title: String,
  onBack: () -> Unit,
  content: @Composable () -> Unit,
) {
  val metrics = fkMetrics()
  Scaffold(
    topBar = { SampleTopBar(title = title, onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(metrics.spacingM),
      verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
    ) {
      content()
    }
  }
}

@Composable
private fun Section(title: String) {
  Text(
    text = title,
    style = fkTextStyle(FkTextStyle.Title3).copy(fontWeight = FontWeight.SemiBold),
    color = fkColor(FkColorRole.OnSurface),
    modifier = Modifier.padding(top = 8.dp),
  )
}

@Composable
private fun Hint(text: String) {
  Text(
    text = text,
    style = fkTextStyle(FkTextStyle.Caption1),
    color = fkColor(FkColorRole.OnSurfaceSecondary),
  )
}

@Composable
private fun ValueLine(text: String) {
  Text(
    text = text,
    style = fkTextStyle(FkTextStyle.Caption1),
    color = fkColor(FkColorRole.OnSurfaceSecondary),
  )
}

@Composable
private fun ToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = fkTextStyle(FkTextStyle.Body),
      color = fkColor(FkColorRole.OnSurface),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

// endregion
