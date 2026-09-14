package com.fk.sample.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkShadowStyle
import com.fk.ui.theme.FkSpacingToken
import com.fk.ui.theme.FkStatusSemantic
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.FkTheme
import com.fk.ui.theme.FkThemeColor
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkShadows
import com.fk.ui.theme.fkStatusColor
import com.fk.ui.theme.fkTextStyle

/**
 * Smoke demo for Phase D1 theme tokens (palette / type / metrics / shadows).
 */
@Composable
fun ThemeDemoScreen(
  onBack: () -> Unit,
) {
  var darkTheme by remember { mutableStateOf(false) }
  var useBrand by remember { mutableStateOf(false) }

  val theme = remember(useBrand) {
    if (useBrand) brandTheme() else FkTheme.Default
  }

  FkTheme(theme = theme, darkTheme = darkTheme) {
    val metrics = fkMetrics()
    val shadows = fkShadows()
    Scaffold(
      topBar = { SampleTopBar(title = "Theme", onBack = onBack) },
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(metrics.spacingM),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingS),
      ) {
        Text("Theme package v${FkTheme.VERSION}", style = fkTextStyle(FkTextStyle.Footnote))
        Text(
          "id=${theme.id} · dark=$darkTheme · brand=$useBrand",
          style = fkTextStyle(FkTextStyle.Caption1),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
          FilterChip(
            selected = darkTheme,
            onClick = { darkTheme = !darkTheme },
            label = { Text(if (darkTheme) "Dark" else "Light") },
          )
          FilterChip(
            selected = useBrand,
            onClick = { useBrand = !useBrand },
            label = { Text(if (useBrand) "Brand" else "Default") },
          )
        }

        SectionTitle("Colors")
        FkColorRole.entries.forEach { role ->
          ColorRow(role.name, fkColor(role))
        }

        SectionTitle("Status")
        FkStatusSemantic.entries.forEach { semantic ->
          ColorRow(semantic.name, fkStatusColor(semantic))
        }

        SectionTitle("Typography")
        FkTextStyle.entries.forEach { style ->
          Text(style.name, style = fkTextStyle(style))
        }

        SectionTitle("Spacing")
        FkSpacingToken.entries.forEach { token ->
          val size = metrics.spacing(token)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingXs),
          ) {
            Text(token.name, modifier = Modifier.weight(1f))
            Box(
              modifier = Modifier
                .size(width = size, height = 12.dp)
                .background(fkColor(FkColorRole.Primary), RoundedCornerShape(2.dp)),
            )
            Text("${size.value.toInt()}dp", style = fkTextStyle(FkTextStyle.Caption2))
          }
        }

        SectionTitle("Radii")
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.spacingS)) {
          RadiusSwatch("S", metrics.radiusSmall, metrics.shapeSmall)
          RadiusSwatch("M", metrics.radiusMedium, metrics.shapeMedium)
          RadiusSwatch("L", metrics.radiusLarge, metrics.shapeLarge)
        }

        SectionTitle("Shadows")
        ShadowCard("Low", shadows.elevationLow, metrics.shapeMedium)
        ShadowCard("Medium", shadows.elevationMedium, metrics.shapeMedium)
        ShadowCard("High", shadows.elevationHigh, metrics.shapeMedium)

        Spacer(modifier = Modifier.height(metrics.spacingXs))
        Button(
          onClick = {
            darkTheme = false
            useBrand = false
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Restore defaults")
        }
      }
    }
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    title,
    style = fkTextStyle(FkTextStyle.Headline),
    modifier = Modifier.padding(top = fkMetrics().spacingXs),
  )
}

@Composable
private fun ColorRow(label: String, color: Color) {
  val metrics = fkMetrics()
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(metrics.spacingS),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .background(color, RoundedCornerShape(6.dp))
        .border(metrics.hairline, fkColor(FkColorRole.Outline), RoundedCornerShape(6.dp)),
    )
    Text(label, modifier = Modifier.weight(1f), style = fkTextStyle(FkTextStyle.Callout))
  }
}

@Composable
private fun RadiusSwatch(
  label: String,
  radius: Dp,
  shape: Shape,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(fkColor(FkColorRole.Primary), shape),
    )
    Text("$label ${radius.value.toInt()}", style = fkTextStyle(FkTextStyle.Caption2))
  }
}

@Composable
private fun ShadowCard(
  label: String,
  style: FkShadowStyle,
  shape: Shape,
) {
  val metrics = fkMetrics()
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(
        elevation = style.radius,
        shape = shape,
        ambientColor = style.color.copy(alpha = style.alpha * 0.5f),
        spotColor = style.color.copy(alpha = style.alpha),
      ),
    shape = shape,
    color = fkColor(FkColorRole.SurfaceElevated),
  ) {
    Text(
      "$label · α=${"%.2f".format(style.alpha)} · r=${style.radius.value.toInt()} · " +
        "oy=${style.offsetY.value.toInt()}",
      modifier = Modifier.padding(metrics.spacingM),
      style = fkTextStyle(FkTextStyle.Callout),
    )
  }
}

private fun brandTheme(): FkTheme {
  val base = FkTheme.Default
  val teal = FkThemeColor(fixed = Color(0xFF0D9488))
  return base.copy(
    id = "brand-sample",
    colors = base.colors.copy(
      primary = teal,
      onPrimary = FkThemeColor(fixed = Color.White),
      statusInfo = teal,
    ),
  )
}
