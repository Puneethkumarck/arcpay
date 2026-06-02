<!-- Title convention: type(scope): imperative summary (#issue) — e.g. feat(compliance): add screening DLT (#197) -->

## Issue

Closes #<!-- issue number -->

## Summary

<!-- What changed and why. Keep it focused on the "why". -->

## Changes

<!-- One bullet per logical change. -->
-

## Checklist

- [ ] `./gradlew build` passes (compile + unit/ArchUnit tests + `spotlessCheck`)
- [ ] `./gradlew spotlessApply` run — code is formatted
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated (`./gradlew :<module>:integrationTest`), if applicable
- [ ] Business/E2E tests added/updated (`./gradlew :<module>:businessTest`), if applicable
- [ ] Adheres to hexagonal layer rules and project coding standards (CLAUDE.md)
