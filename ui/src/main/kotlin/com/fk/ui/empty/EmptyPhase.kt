package com.fk.ui.empty

/**
 * Presentation phase for empty-state surfaces.
 *
 * Conceptually aligned with iOS `FKEmptyStatePhase`.
 * [Content] hides the overlay without tearing down host layout.
 */
sealed class EmptyPhase {
  /** Normal business UI — empty-state surface hidden. */
  data object Content : EmptyPhase()

  /** Spinner + optional copy (initial load). */
  data object Loading : EmptyPhase()

  /** Empty list / no results — illustration + copy + optional actions. */
  data object Empty : EmptyPhase()

  /** Failure — copy + required retry (enforced when primary is missing). */
  data object Error : EmptyPhase()

  /** Domain-specific state rendered with the same layout pipeline. */
  data class Custom(val id: String) : EmptyPhase()
}
