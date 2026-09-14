package com.fk.ui.empty

/**
 * Semantic empty-state categories for copy presets, analytics, and resolution.
 *
 * Distinct from [EmptyPhase]: phase controls rendering; type communicates intent.
 * [analyticsId] values stay stable (iOS `FKEmptyStateType.rawValue` parity).
 */
enum class EmptyType(val analyticsId: String) {
  Empty("empty"),
  NoResults("no_results"),
  Error("error"),
  Offline("offline"),
  PermissionDenied("permission_denied"),
  NotFound("not_found"),
  Maintenance("maintenance"),
  Loading("loading"),
  NewUser("new_user"),
}

/**
 * Input snapshot for [EmptyResolver].
 *
 * UI-agnostic — safe to build from view models without Compose.
 */
data class EmptyInputs(
  val dataLength: Int? = null,
  val isLoading: Boolean = false,
  val errorDescription: String? = null,
  val searchQuery: String? = null,
  val hasPermission: Boolean? = null,
  val isOffline: Boolean? = null,
  val isNewUser: Boolean? = null,
)

/** Resolver output: show a semantic type, or render normal content. */
sealed class EmptyResolution {
  data object None : EmptyResolution()
  data class Show(val type: EmptyType) : EmptyResolution()
}

/**
 * Resolves a single display type from multiple signals (severity-first).
 *
 * Priority: permission → offline → loading → error → newUser → has data →
 * noResults → empty. Conceptually aligned with iOS `FKEmptyStateResolver`.
 */
object EmptyResolver {
  /** Resolves [input] into [EmptyResolution]. */
  fun resolve(input: EmptyInputs): EmptyResolution {
    if (input.hasPermission == false) return EmptyResolution.Show(EmptyType.PermissionDenied)
    if (input.isOffline == true) return EmptyResolution.Show(EmptyType.Offline)
    if (input.isLoading) return EmptyResolution.Show(EmptyType.Loading)
    if (!input.errorDescription.isNullOrEmpty()) return EmptyResolution.Show(EmptyType.Error)
    if (input.isNewUser == true) return EmptyResolution.Show(EmptyType.NewUser)

    val length = input.dataLength
    if (length != null && length > 0) return EmptyResolution.None

    if (!input.searchQuery.isNullOrEmpty()) return EmptyResolution.Show(EmptyType.NoResults)
    return EmptyResolution.Show(EmptyType.Empty)
  }
}
