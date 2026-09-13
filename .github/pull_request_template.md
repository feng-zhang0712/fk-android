## Summary

<!-- What changed and why (1–3 bullets). -->

-

## Module

<!-- Which library module(s) this PR touches. -->

- [ ] `:core`
- [ ] `:ui`
- [ ] `:business`
- [ ] `:sample` / docs / build

## Test plan

- [ ] Local verify: `./gradlew :core:assembleRelease :ui:assembleRelease :business:assembleRelease`
- [ ] `:sample` updated when **public API** changed (if applicable)
- [ ] Component guide consulted / updated when adding a new component area (`docs/component-guide.md`)

## New public API

<!-- Required when adding or changing public API. -->

- [ ] Public types documented in English (`/** */` / KDoc)
- [ ] No speculative dependencies added (only what this component needs)

## Migration / breaking changes

<!-- API or behavior changes integrators must know. Omit if none. -->

None.
