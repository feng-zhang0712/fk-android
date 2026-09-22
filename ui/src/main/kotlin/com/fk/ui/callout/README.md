# Callout (`com.fk.ui.callout`)

Anchored speech-bubble overlays for Compose: one shared layout engine and chrome layer, with **`FkTooltip`** and **`FkPopover`** as the recommended entry points.

## API layers

| Layer | Types | When to use |
|-------|--------|-------------|
| **Recommended** | `FkTooltip`, `FkPopover` | Normal app integration. Presets set `kind`, default configuration, and semantic helpers (`showMenu`, `showCoachMark`, …). |
| **Advanced** | `FkCallout`, `CalloutRequest` | Custom content combinations, `showOrUpdate`, runtime `update`, or presentation logic that does not map cleanly to a preset overload. |
| **Shared models** | `CalloutConfiguration`, `CalloutHandle`, menu / action / coach-mark models | Used by both presets and advanced APIs. |

## When to use which API

| Need | Recommended API |
|------|-----------------|
| Short hint near a control, auto-dismiss | `FkTooltip` |
| Rich card, menu, coach mark, custom panel | `FkPopover` |
| Arbitrary `CalloutContent`, builder hooks, or in-place update | `FkCallout` |
| Global status / HUD | `Toast` / `Hud` |
| Modal sheet | `FkBottomSheet` / `FkCenterSheet` |

## Layout

| Type | Role |
|------|------|
| `Callout` | Package hub + global preset stores |
| `CalloutController` / `rememberCalloutController` | Session presenter |
| `CalloutHost` / `CalloutOverlay` | Compose overlay host |
| `CalloutAnchorState` / `Modifier.calloutAnchor` | Anchor tracking |
| `CalloutConfiguration` / `CalloutAppearance` | Placement, chrome, backdrop, keyboard, policy |
| `CalloutContent` | Message, title/body, icon, actions, header, coach mark, menu, custom |
| `FkTooltip` / `FkPopover` / `FkCallout` | Public façades |

## Configuration defaults

| Global store | Role |
|--------------|------|
| `FkTooltip.defaultConfiguration` | Tooltip preset (`kind: Tooltip`) |
| `FkPopover.defaultConfiguration` | Popover preset (`kind: Popover`) |
| `FkPopover.menuConfiguration` | Menu / select preset |
| `FkCallout.defaultConfiguration` | Baseline for advanced `FkCallout.show` |

Prefer `CalloutConfiguration.tooltipDefault()`, `popoverDefault()`, or `menuDefault()` for per-request options.

### Placement vs configuration

`FkTooltip.show` / `FkPopover.show` always set `kind` on the resolved configuration. When the façade `placement` parameter is not `Automatic`, it **overrides** `configuration.placement` even when you pass a custom `configuration`. Use `Automatic` on the façade when you want full control via `configuration` only.

### Concurrent presentations

Default `presentationPolicy` is `ReplaceActive` (one bubble). Set `AllowConcurrent` to stack multiple sessions. `dismissActive()` dismisses **all** active sessions.

## Content types

| `CalloutContent` | Design use |
|------------------|------------|
| `Message` | Tooltip copy |
| `TitleSubtitle` | Simple popover |
| `IconMessage` | Icon + tip row |
| `MessageWithActions` | Tip + footer button(s) |
| `HeaderPanel` | Colored header + body |
| `CoachMark` | Onboarding card (title, close, CTA) |
| `Menu` | Dropdown / action / account menus |
| `Custom` | Fully custom Compose content |

## Usage

```kotlin
val controller = rememberCalloutController()
val anchor = rememberCalloutAnchorState()

CalloutHost(controller) {
  Button(
    onClick = {
      FkPopover.show(
        controller = controller,
        title = "Details",
        message = "Body copy inside the card.",
        anchor = anchor,
        placement = CalloutPlacement.Bottom,
      )
    },
    modifier = Modifier.calloutAnchor(anchor),
  ) {
    Text("Anchor")
  }
}

FkTooltip.show(controller, "Hint", anchor, placement = CalloutPlacement.Top)

FkPopover.showMenu(
  controller,
  menu = CalloutMenu(
    sections = listOf(
      CalloutMenuSection(
        items = listOf(
          CalloutMenuItem(title = "Edit"),
          CalloutMenuItem(title = "Delete", isDestructive = true),
        ),
      ),
    ),
  ),
  anchor = anchor,
  onSelect = { /* … */ },
)

FkPopover.showCoachMark(
  controller,
  content = CalloutCoachMarkContent(
    title = "Tap to switch profiles",
    message = "Switch between your profiles for unique app experiences",
  ),
  anchor = anchor,
  primaryAction = { /* continue */ },
)

FkPopover.dismissActive(controller)
```

## Notes

- Bubble chrome includes a triangular beak; twelve placements cover edges and corners, with automatic flip when space is tight.
- Popover defaults: light card, 12dp corners, shadow, hairline border, max width 320dp, tap-outside dismiss.
- Tooltip defaults: dark compact surface, max width 240dp, auto-dismiss after 3s.
- Coach marks enable a dimmed backdrop with an optional anchor spotlight by default.
- Keyboard avoidance: `Relayout` shrinks the usable layout when the IME is visible; `Dismiss` closes the callout when the keyboard opens.
- Wrap the screen (or a high subtree) in `CalloutHost` so overlays share the same coordinate space as anchors.
