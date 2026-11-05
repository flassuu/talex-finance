# AGENTS.md — Talex

## What this is

Talex is a privacy-first personal finance application for Android: accounts,
income and expenses, budgets, savings goals, assets, liabilities and analytics,
stored locally on the device. The repository currently contains documentation
only. No application code, Gradle build, database schema or synchronization
protocol exists yet.

## Current state

- Phase 0: documentation foundation.
- Application ID, Android baseline and the portable storage model are recorded
  in `docs/decisions/`. Modularization remains open.
- The stack in `docs/technical/architecture.md` is preliminary, not binding.
- There are no build, test or lint commands. Do not invent them.
- When a Gradle build is added, replace the command table below with real,
  verified commands.

## Commands

| Task | Command |
|---|---|
| Build | Not available yet |
| Test | Not available yet |
| Lint / format | Not available yet |

## Layout

```text
AGENTS.md              Agent and contributor rules
README.md              Product entry point
CHANGELOG.md           Keep a Changelog
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
2. **Do not finalize what is open.** The database schema and the synchronization
   protocol are not decided. Treat `docs/open-questions.md` as the list of
   unresolved choices.
3. **Keep status explicit.** Mark statements as established, provisional or open.
   Do not present preliminary technology choices as immutable decisions.
4. **One fact, one place.** Put explanations in `docs/`; `AGENTS.md` holds rules
   and commands. Link instead of duplicating.
5. **Update docs in the same change.** If behavior, structure or a decision
   changes, update the owning document and, for a significant decision, add an
   ADR from `docs/templates/adr.md`.
6. **REF01 is a functional reference only.** Do not copy its code, file
   structure, identifiers or implementation.
7. **No secrets or personal data.** Never commit keystores, database files,
   exports, `.env` files, tokens or real financial data.
8. **No license yet.** Do not add a `LICENSE` file or license headers until the
   distribution model is decided.
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

## Vault context

| Read | Path (relative to `$HOME`) |
|---|---|
| Intent | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Description.md` |
| Ideas | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Ideas.md` |
| Work log | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Sessions.md` |
| Repository facts | `Documents/Obsidian/kernel.v02/06 - Projects/102_TALEX/Repo.md` |

- At session start, read `Description.md` and the last two entries of
  `Sessions.md`.
- `Ideas.md` is read-only. Never rewrite or delete it.
- At session end, append one entry to `Sessions.md` before `## Related`:
  `## YYYY-MM-DD — title`, then `Done`, `Decided`, `Next`, one line each, in
  Russian.
- Do not edit other vault notes. `docs/` and `github/` in the vault are symlinks
  back to this repository; write through the repository, never through a link.
