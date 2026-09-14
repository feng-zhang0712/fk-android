package com.fk.ui.empty

/**
 * Semantic role for an action slot.
 *
 * Route interactions by [EmptyAction.id]; treat [kind] as a presentation hint.
 * Conceptually aligned with iOS `FKEmptyStateActionKind`.
 */
enum class EmptyActionKind {
  Primary,
  Secondary,
  Tertiary,
  Link,
}

/**
 * Immutable action payload rendered by empty-state surfaces.
 *
 * Keep [id] stable across releases. [payload] is optional analytics metadata.
 */
data class EmptyAction(
  val id: String,
  val title: String,
  val kind: EmptyActionKind,
  val isEnabled: Boolean = true,
  val isLoading: Boolean = false,
  val payload: Map<String, String> = emptyMap(),
)

/**
 * Fixed-size action container (max three) for predictable hierarchy.
 *
 * Conceptually aligned with iOS `FKEmptyStateActionSet`.
 */
data class EmptyActionSet(
  val primary: EmptyAction? = null,
  val secondary: EmptyAction? = null,
  val tertiary: EmptyAction? = null,
) {
  /** Actions in rendering priority order. */
  val all: List<EmptyAction>
    get() = listOfNotNull(primary, secondary, tertiary)

  /** `true` when no actions are configured. */
  val isEmpty: Boolean get() = all.isEmpty()

  companion object {
    /** Creates a set with a single primary action. */
    fun primary(
      title: String,
      id: String = "primary",
      kind: EmptyActionKind = EmptyActionKind.Primary,
    ): EmptyActionSet = EmptyActionSet(
      primary = EmptyAction(id = id, title = title, kind = kind),
    )
  }
}
