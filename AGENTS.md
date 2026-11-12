# AGENTS.md – Talex

## What this is

Talex is a privacy-first personal finance application for Android: accounts,
income and expenses, budgets, savings goals, assets, liabilities and analytics,
stored locally on the device. The repository currently contains documentation
only. No application code, Gradle build or database schema exists yet.

## Current state

- Phases 0-3 complete; the Android app covers the ledger, budgets, goals, assets
  and liabilities, and the full capital overview.
- One `:app` module: Kotlin 2.2.20, Compose, Room 2.8.5 over SQLCipher 4.20.0,
  Argon2id via `argon2kt`, Hilt. Versions live in `gradle/libs.versions.toml`.
- Schema v4 includes accounts, entries, categories, tags, settings, transfers,
  rates, split lines, recurring rules, budgets, goals and valuations (ADR-0020,
  ADR-0021).
- The encrypted database uses a `talex.db.meta` sidecar for the Argon2 salt and
  parameters (ADR-0019). Biometric unlock is local convenience only.
- Unit tests and lint pass; instrumentation tests run on a physical device.

## Commands

| Task | Command |
|---|---|
| Build debug APK | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug` |
| Install on device | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest` |
| Instrumentation tests | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:connectedDebugAndroidTest` (needs a device) |
| Lint | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug` |

JDK 21 is required: Gradle 8.14 does not run on the newer system JDK.

Agent command: `/publish` starts the GitHub publishing flow described in
`docs/publishing.md`.

## Layout

```text
AGENTS.md              Agent and contributor rules
README.md              Product entry point
CHANGELOG.md           Keep a Changelog
app/                   Android application module
gradle/                Version catalog and Gradle wrapper
docs/                  Source of truth for product and technical documentation
  README.md            Documentation index
  product/             Product concept and market research
  technical/           Architecture, database, security, conventions
  decisions/           Decision log and ADR process
  templates/           ADR and feature-spec templates
  open-questions.md    Single register of unresolved questions
```

## Rules

1. **Documentation before implementation.** Do not add application code, Gradle
   modules or dependencies unless the user explicitly asks for them.
2. **Do not finalize what is open.** The database schema is not decided, and
   there is no device synchronization (D-035). Treat `docs/open-questions.md` as
   the list of unresolved choices.
3. **Keep status explicit.** Mark statements as established, provisional or open.
   Do not present preliminary technology choices as immutable decisions.
4. **One fact, one place.** Put explanations in `docs/`; `AGENTS.md` holds rules
   and commands. Link instead of duplicating.
5. **Update docs in the same change.** If behavior, structure or a decision
   changes, update the owning document and, for a significant decision, add an
   ADR from `docs/templates/adr.md`.
6. **REF01 is a functional reference only.** Do not copy its code, file
   structure, identifiers or implementation. Its identity and detailed behavior
   notes stay outside the repository; see `docs/product/reference-ref01.md`.
7. **No secrets or personal data.** Never commit keystores, database files,
   exports, `.env` files, tokens or real financial data.
8. **License is GPL-3.0.** Do not change the license or add a dependency with
   incompatible terms without a decision. Keep the `LICENSE` file intact.
9. **English in the repository.** Code, comments, commit messages, documentation
   and identifiers are English. Vault notes are Russian.
10. **Conventional commits.** Use `feat:`, `fix:`, `docs:`, `refactor:`, `test:`,
    `chore:`. Keep `main` releasable. Do not commit build artifacts.
11. **Ask before scope expansion.** A useful change outside the request is
    reported, not made silently. Ask before adding a dependency or tool.
12. **Backups are user data.** Never add silent cloud backup, telemetry or
    network access.

## Documentation rules

- Every file starts with a one-line purpose and a "Read this when" section.
- No code listings unless a short example is the clearest explanation.
- Link to the owning file instead of repeating a fact.
- Archive obsolete documents; do not silently rewrite history. Supersede an ADR
  rather than editing an accepted one.
- Use the en dash `–` for dashes in prose, as in the vault notes. Use the hyphen
  `-` in numeric ranges. Do not use the em dash (`U+2014`).
- No emojis in repository text.
- Do not add a `## Related` section to files in `docs/`. `docs/README.md` is the
  only navigation index; the symlinked vault view supplies backlinks.

## Vault context

| Read | Path (relative to `$HOME`) |
|---|---|
| Intent | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Description.md` |
| Ideas | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Ideas.md` |
| Work log | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Sessions.md` |
| Repository facts | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Repo.md` |

- At session start, read `Description.md` and the two most recent entries of
  `Sessions.md` (the top two).
- `Ideas.md` is read-only. Never rewrite or delete it.
- At session end, prepend one entry to `Sessions.md`, newest first like a
  changelog: `## YYYY-MM-DD – title`, then `Done`, `Decided`, `Next`, one line
  each, in Russian.
- Write one entry per day or work block, not one per task. Group related work so
  a single day does not end up with many entries. This overrides the
  project-vault default of appending at the end.
- Do not edit other vault notes. `docs/` and `github/` in the vault are symlinks
  back to this repository; write through the repository, never through a link.
