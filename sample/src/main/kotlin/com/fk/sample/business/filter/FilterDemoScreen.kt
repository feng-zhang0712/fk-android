package com.fk.sample.business.filter

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.fk.business.filter.FilterConfiguration
import com.fk.business.filter.FilterHost
import com.fk.business.filter.FilterId
import com.fk.business.filter.FilterKit
import com.fk.business.filter.FilterOptionItem
import com.fk.business.filter.FilterPanelContent
import com.fk.business.filter.FilterPanelKind
import com.fk.business.filter.FilterSection
import com.fk.business.filter.FilterSelectionMode
import com.fk.business.filter.FilterTab
import com.fk.business.filter.TwoColumnFilterModel
import com.fk.business.filter.TwoColumnPanelConfig
import com.fk.business.filter.TwoColumnRightHeaderBehavior
import com.fk.business.filter.TwoColumnSingleSelectionScope
import com.fk.business.filter.rememberFilterController
import com.fk.sample.ui.SampleTopBar
import com.fk.ui.theme.FkColorRole
import com.fk.ui.theme.FkSpacingToken
import com.fk.ui.theme.FkTextStyle
import com.fk.ui.theme.fkColor
import com.fk.ui.theme.fkMetrics
import com.fk.ui.theme.fkTextStyle

/**
 * Full demo for Phase F2 filter (strip + hierarchy / dual / tags / list panels).
 */
@Composable
fun FilterDemoScreen(
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val metrics = fkMetrics()
  val tabs = remember {
    listOf(
      FilterTab(id = "sort", title = "Sort", panelKind = FilterPanelKind.SingleList),
      FilterTab(id = "knowledge", title = "Knowledge", panelKind = FilterPanelKind.Hierarchy),
      FilterTab(id = "course", title = "Course", panelKind = FilterPanelKind.DualHierarchy),
      FilterTab(
        id = "tags",
        title = "Tags",
        panelKind = FilterPanelKind.Tags,
        allowsMultipleSelection = true,
      ),
      FilterTab(id = "custom", title = "Custom", panelKind = FilterPanelKind.Custom),
    )
  }
  val controller = rememberFilterController(
    tabs = tabs,
    configuration = FilterConfiguration(),
  )
  var panels by remember {
    mutableStateOf(
      mapOf(
        "sort" to FilterPanelContent.SingleList(
          section = FilterSection(
            id = FilterId("sort"),
            selectionMode = FilterSelectionMode.Single,
            items = listOf(
              FilterOptionItem(FilterId("latest"), "Latest", isSelected = true),
              FilterOptionItem(FilterId("hot"), "Most popular"),
              FilterOptionItem(FilterId("rating"), "Highest rated"),
            ),
          ),
        ),
        "knowledge" to FilterPanelContent.Hierarchy(
          model = sampleKnowledgeModel(),
          config = TwoColumnPanelConfig(
            singleSelectionScope = TwoColumnSingleSelectionScope.WithinSection,
            rightHeaderBehavior = TwoColumnRightHeaderBehavior.ToggleCollapse,
          ),
        ),
        "course" to FilterPanelContent.DualHierarchy(
          model = sampleCourseModel(),
        ),
        "tags" to FilterPanelContent.Tags(
          sections = listOf(
            FilterSection(
              id = FilterId("platform"),
              title = "Platform",
              selectionMode = FilterSelectionMode.Multiple,
              items = listOf(
                FilterOptionItem(FilterId("android"), "Android", isSelected = true),
                FilterOptionItem(FilterId("ios"), "iOS"),
                FilterOptionItem(FilterId("web"), "Web"),
                FilterOptionItem(FilterId("desktop"), "Desktop", isEnabled = false),
              ),
            ),
            FilterSection(
              id = FilterId("level"),
              title = "Level",
              selectionMode = FilterSelectionMode.Multiple,
              items = listOf(
                FilterOptionItem(FilterId("beginner"), "Beginner"),
                FilterOptionItem(FilterId("intermediate"), "Intermediate"),
                FilterOptionItem(FilterId("advanced"), "Advanced"),
              ),
            ),
          ),
          columns = 3,
        ),
        "custom" to FilterPanelContent.Custom(key = "promo"),
      ),
    )
  }
  var lastEvent by remember { mutableStateOf("Tap a filter tab to expand a panel.") }

  Scaffold(
    topBar = {
      SampleTopBar(
        title = "Filter",
        onBack = onBack,
      )
    },
  ) { padding ->
    FilterHost(
      controller = controller,
      panelContents = panels,
      onPanelContentChange = { tabId, content ->
        panels = panels + (tabId to content)
      },
      onSelection = { ctx ->
        lastEvent =
          "tab=${ctx.tabId} kind=${ctx.panelKind} item=${ctx.selection.item.title} " +
            "mode=${ctx.selection.effectiveMode}"
        Toast.makeText(context, lastEvent, Toast.LENGTH_SHORT).show()
      },
      customPanel = {
        Column(modifier = Modifier.padding(metrics.spacing(FkSpacingToken.L))) {
          Text(
            text = "Custom panel slot",
            style = fkTextStyle(FkTextStyle.Headline),
            fontWeight = FontWeight.SemiBold,
            color = fkColor(FkColorRole.OnSurface),
          )
          Text(
            text = "Apps render bespoke content here (promo, search, date range, …).",
            style = fkTextStyle(FkTextStyle.Body),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            modifier = Modifier.padding(top = metrics.spacing(FkSpacingToken.S)),
          )
          Text(
            text = "FilterKit ${FilterKit.VERSION}",
            style = fkTextStyle(FkTextStyle.Caption1),
            color = fkColor(FkColorRole.OnSurfaceSecondary),
            modifier = Modifier.padding(top = metrics.spacing(FkSpacingToken.M)),
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(metrics.spacing(FkSpacingToken.L)),
        verticalArrangement = Arrangement.spacedBy(metrics.spacing(FkSpacingToken.M)),
      ) {
        Text(
          text = "Results area",
          style = fkTextStyle(FkTextStyle.Title3),
          fontWeight = FontWeight.SemiBold,
          color = fkColor(FkColorRole.OnSurface),
        )
        Text(
          text = lastEvent,
          style = fkTextStyle(FkTextStyle.Body),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        Text(
          text = "Single-select panels override the tab title and auto-collapse. " +
            "Tags allow multi-select and stay open until you dismiss the scrim.",
          style = fkTextStyle(FkTextStyle.Subheadline),
          color = fkColor(FkColorRole.OnSurfaceSecondary),
        )
        repeat(12) { index ->
          Text(
            text = "Mock result #${index + 1}",
            style = fkTextStyle(FkTextStyle.Body),
            color = fkColor(FkColorRole.OnSurface),
            modifier = Modifier.padding(vertical = metrics.spacing(FkSpacingToken.S)),
          )
        }
      }
    }
  }
}

private fun sampleKnowledgeModel(): TwoColumnFilterModel {
  val lang = FilterId("lang")
  val design = FilterId("design")
  return TwoColumnFilterModel(
    categories = listOf(
      TwoColumnFilterModel.Category(lang, "Languages", isSelected = true),
      TwoColumnFilterModel.Category(design, "Design"),
      TwoColumnFilterModel.Category(FilterId("empty"), "Empty"),
    ),
    sectionsByCategoryId = mapOf(
      lang to listOf(
        FilterSection(
          id = FilterId("jvm"),
          title = "JVM",
          selectionMode = FilterSelectionMode.Single,
          items = listOf(
            FilterOptionItem(FilterId("kotlin"), "Kotlin", isSelected = true),
            FilterOptionItem(FilterId("java"), "Java"),
            FilterOptionItem(FilterId("scala"), "Scala"),
          ),
        ),
        FilterSection(
          id = FilterId("mobile"),
          title = "Mobile",
          selectionMode = FilterSelectionMode.Single,
          items = listOf(
            FilterOptionItem(FilterId("swift"), "Swift"),
            FilterOptionItem(FilterId("dart"), "Dart"),
          ),
        ),
      ),
      design to listOf(
        FilterSection(
          id = FilterId("ui"),
          title = "UI",
          selectionMode = FilterSelectionMode.Single,
          items = listOf(
            FilterOptionItem(FilterId("compose"), "Compose"),
            FilterOptionItem(FilterId("swiftui"), "SwiftUI"),
          ),
        ),
      ),
      FilterId("empty") to emptyList(),
    ),
  )
}

private fun sampleCourseModel(): TwoColumnFilterModel {
  val all = FilterId("all")
  val eng = FilterId("eng")
  return TwoColumnFilterModel(
    categories = listOf(
      TwoColumnFilterModel.Category(all, "All", isSelected = true),
      TwoColumnFilterModel.Category(eng, "Engineering"),
    ),
    sectionsByCategoryId = mapOf(
      all to listOf(
        FilterSection(
          id = FilterId("tracks"),
          title = "All tracks",
          selectionMode = FilterSelectionMode.Single,
          items = listOf(
            FilterOptionItem(FilterId("android-track"), "Android"),
            FilterOptionItem(FilterId("ios-track"), "iOS"),
            FilterOptionItem(FilterId("backend"), "Backend"),
            FilterOptionItem(FilterId("ml"), "ML"),
          ),
        ),
      ),
      eng to listOf(
        FilterSection(
          id = FilterId("eng-tracks"),
          title = "Engineering",
          selectionMode = FilterSelectionMode.Single,
          items = listOf(
            FilterOptionItem(FilterId("systems"), "Systems"),
            FilterOptionItem(FilterId("data"), "Data"),
          ),
        ),
      ),
    ),
  )
}
