package com.fk.ui.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fk.ui.theme.FkStatusSemantic

/**
 * Design-system widgets: Avatar, Chip / Tag / ChipGroup, StatusPill.
 *
 * Conceptually aligned with iOS FKUIKit Widgets (Compose, not UIKit).
 */
object WidgetKit {
  /** Package semantic version (keep in sync with library version when publishing). */
  const val VERSION: String = "0.1.3"
}

/** Avatar diameter presets (iOS `FKAvatarSize`). */
enum class AvatarSize(val diameter: Dp) {
  Xs(24.dp),
  S(32.dp),
  M(40.dp),
  L(48.dp),
  Xl(72.dp),
}

/** Avatar clip shape. */
enum class AvatarShape {
  Circle,
  RoundedRect,
}

/** User presence for avatar chrome (not workflow status). */
enum class PresenceState {
  Online,
  Offline,
  Busy,
  Away,
}

/** Diameter presets for [FkPresenceDot]. */
enum class PresenceDotSize(val diameter: Dp) {
  S(8.dp),
  M(10.dp),
  L(12.dp),
}

/** Interactive chip height presets. */
enum class ChipSize(val height: Dp) {
  Xs(22.dp),
  S(30.dp),
  M(36.dp),
}

/** Interactive chip behavior (iOS `FKChipMode`) — maps to Material chip types. */
enum class ChipMode {
  /** Maps to Material [androidx.compose.material3.FilterChip]; tap toggles selection. */
  Filter,
  /** Same Material backing as [Filter]; typically single-select inside [FkChipGroup]. */
  Choice,
  /** Maps to Material [androidx.compose.material3.InputChip]; optional remove. */
  Input,
  /** Maps to Material [androidx.compose.material3.SuggestionChip]; one-shot action. */
  Suggestion,
}

/** Read-only tag / metadata variant (marketing palette, not workflow). */
enum class TagVariant {
  Neutral,
  Brand,
  Success,
  Warning,
  Error,
  Outline,
}

/** Chip group selection orchestration. */
sealed class ChipSelectionMode {
  data object None : ChipSelectionMode()
  data object Single : ChipSelectionMode()
  data class Multiple(val max: Int? = null) : ChipSelectionMode()
}

/** Row model for [FkChipGroup]. */
data class ChipItem(
  val id: String,
  val title: String,
  val enabled: Boolean = true,
  val removable: Boolean = false,
)

/** Status pill height presets. */
enum class StatusPillSize(val height: Dp) {
  S(28.dp),
  M(32.dp),
}

/** Alias for workflow status styling (shared with theme tokens). */
typealias StatusPillStyle = FkStatusSemantic
