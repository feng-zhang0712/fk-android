package com.fk.ui.empty

import androidx.compose.ui.unit.dp

/**
 * High-level product scenarios for [EmptyConfiguration.scenario].
 *
 * Conceptually aligned with iOS `FKEmptyStateScenario`.
 */
enum class EmptyScenario {
  NoNetwork,
  NoSearchResult,
  NoFavorites,
  NoOrders,
  NoMessages,
  LoadFailed,
  NoPermission,
  NotLoggedIn,
}

/**
 * Aggregate configuration for empty-state surfaces.
 *
 * Conceptually aligned with iOS `FKEmptyStateConfiguration`.
 */
data class EmptyConfiguration(
  val phase: EmptyPhase = EmptyPhase.Empty,
  val type: EmptyType = EmptyType.Empty,
  val content: EmptyContent = EmptyContent(),
  val layout: EmptyLayout = EmptyLayout(),
  val actions: EmptyActionSet = EmptyActionSet(),
  val presentation: EmptyPresentation = EmptyPresentation(),
) {
  companion object {
    /** Default retry title when [EmptyPhase.Error] has no primary action. */
    const val DefaultRetryTitle: String = "Retry"

    /** Builds a configuration from common content fields. */
    fun of(
      phase: EmptyPhase = EmptyPhase.Empty,
      type: EmptyType = EmptyType.Empty,
      title: String? = null,
      description: String? = null,
      primaryActionTitle: String? = null,
      primaryActionId: String = "primary",
    ): EmptyConfiguration {
      val actions = if (!primaryActionTitle.isNullOrEmpty()) {
        EmptyActionSet.primary(primaryActionTitle, id = primaryActionId)
      } else {
        EmptyActionSet()
      }
      return EmptyConfiguration(
        phase = phase,
        type = type,
        content = EmptyContent(title = title, description = description),
        actions = actions,
      )
    }

    /** Returns a configuration pre-filled for [scenario]. */
    fun scenario(scenario: EmptyScenario): EmptyConfiguration = when (scenario) {
      EmptyScenario.NoNetwork -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.Offline,
        content = EmptyContent(
          title = "No network connection",
          description = "Check your connection and try again.",
        ),
        actions = EmptyActionSet.primary("Try again", id = "retry"),
      )
      EmptyScenario.NoSearchResult -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.NoResults,
        content = EmptyContent(
          title = "No results",
          description = "Try a different search term.",
        ),
      )
      EmptyScenario.NoFavorites -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.Empty,
        content = EmptyContent(
          title = "No favorites yet",
          description = "Items you favorite will show up here.",
        ),
        actions = EmptyActionSet.primary("Browse"),
      )
      EmptyScenario.NoOrders -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.Empty,
        content = EmptyContent(
          title = "No orders",
          description = "Orders you place will appear here.",
        ),
        actions = EmptyActionSet.primary("Start shopping"),
      )
      EmptyScenario.NoMessages -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.Empty,
        content = EmptyContent(
          title = "No messages",
          description = "You're all caught up.",
        ),
      )
      EmptyScenario.LoadFailed -> EmptyConfiguration(
        phase = EmptyPhase.Error,
        type = EmptyType.Error,
        content = EmptyContent(
          title = "Something went wrong",
          description = "We couldn't load this content.",
        ),
        actions = EmptyActionSet.primary(DefaultRetryTitle, id = "retry"),
      )
      EmptyScenario.NoPermission -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.PermissionDenied,
        content = EmptyContent(
          title = "Permission required",
          description = "Allow access to continue.",
        ),
        actions = EmptyActionSet.primary("OK"),
      )
      EmptyScenario.NotLoggedIn -> EmptyConfiguration(
        phase = EmptyPhase.Empty,
        type = EmptyType.NewUser,
        content = EmptyContent(
          title = "Sign in to continue",
          description = "Create an account or sign in to see this content.",
        ),
        layout = EmptyLayout(maxContentWidth = 360.dp),
        actions = EmptyActionSet.primary("Sign in"),
      )
    }

    /**
     * Builds a configuration from [EmptyResolver] output and optional input metadata.
     *
     * Returns [EmptyPhase.Content] when the resolver yields [EmptyResolution.None].
     * Error descriptions from [EmptyInputs.errorDescription] override body copy for errors.
     */
    fun resolved(from: EmptyInputs): EmptyConfiguration =
      when (val resolution = EmptyResolver.resolve(from)) {
        EmptyResolution.None -> EmptyConfiguration(phase = EmptyPhase.Content)
        is EmptyResolution.Show -> configurationFor(resolution.type, from)
      }

    /** Custom business state with optional primary action. */
    fun customState(
      identifier: String,
      title: String?,
      description: String? = null,
      buttonTitle: String? = null,
      buttonId: String = "primary",
    ): EmptyConfiguration = of(
      phase = EmptyPhase.Custom(identifier),
      type = EmptyType.Empty,
      title = title,
      description = description,
      primaryActionTitle = buttonTitle,
      primaryActionId = buttonId,
    )

    private fun configurationFor(type: EmptyType, input: EmptyInputs): EmptyConfiguration =
      when (type) {
        EmptyType.Offline -> scenario(EmptyScenario.NoNetwork)
        EmptyType.NoResults -> scenario(EmptyScenario.NoSearchResult)
        EmptyType.Error -> {
          val base = scenario(EmptyScenario.LoadFailed)
          val detail = input.errorDescription
          if (!detail.isNullOrEmpty()) {
            base.copy(content = base.content.copy(description = detail))
          } else {
            base
          }
        }
        EmptyType.PermissionDenied -> scenario(EmptyScenario.NoPermission)
        EmptyType.NewUser -> scenario(EmptyScenario.NotLoggedIn)
        EmptyType.Empty -> scenario(EmptyScenario.NoMessages)
        EmptyType.Loading -> EmptyConfiguration(
          phase = EmptyPhase.Loading,
          type = EmptyType.Loading,
          content = EmptyContent(loadingMessage = "Loading…"),
        )
        EmptyType.NotFound -> scenario(EmptyScenario.NoMessages).copy(type = EmptyType.NotFound)
        EmptyType.Maintenance -> customState(
          identifier = "maintenance",
          title = "Under maintenance",
          description = "Please try again later.",
          buttonTitle = DefaultRetryTitle,
          buttonId = "retry",
        ).copy(type = EmptyType.Maintenance)
      }
  }
}

/**
 * Ensures [EmptyPhase.Error] always has a primary retry action.
 */
internal fun EmptyConfiguration.withEnforcedRetry(): EmptyConfiguration {
  if (phase !is EmptyPhase.Error) return this
  val primary = actions.primary
  if (primary != null && primary.title.isNotBlank()) return this
  return copy(actions = EmptyActionSet.primary(EmptyConfiguration.DefaultRetryTitle, id = "retry"))
}

internal fun EmptyDensity.spacingScale(): Float = when (this) {
  EmptyDensity.Compact -> 0.75f
  EmptyDensity.Regular -> 1f
  EmptyDensity.Comfortable -> 1.25f
}

internal fun EmptyDensity.typeScale(): Float = when (this) {
  EmptyDensity.Compact -> 0.9f
  EmptyDensity.Regular -> 1f
  EmptyDensity.Comfortable -> 1.1f
}
