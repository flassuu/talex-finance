# Development Conventions

Read this when you write code, tests, commits or documentation in this
repository.

**Status:** partially established. Language and documentation rules are binding;
tooling and code style will be confirmed when the Android project is created.

## Language

- Code, identifiers, comments, commit messages and repository documentation are
  English.
- Vault notes are Russian. The session log is Russian.
- User-facing localization is open (Q-003). Do not hard-code strings in a way
  that blocks localization.

## Stack

Preliminary and subject to validation:

| Concern | Direction |
|---|---|
| Language | Kotlin |
| Application ID | `io.github.flassuu.talex` — [ADR-0001](../decisions/0001-application-id.md) |
| Min SDK | 26 — [ADR-0002](../decisions/0002-android-baseline.md) |
| UI | Jetpack Compose |
| Build | Gradle with Kotlin DSL |
| Persistence | Room / SQLite |
| Encryption | SQLCipher |
| Authentication | AndroidX Biometric with app PIN fallback — [ADR-0003](../decisions/0003-authentication-fallback.md) |
| Keys | Android Keystore |
| Async | Coroutines / Flow |
| DI | Hilt |

No versions are pinned yet. Do not add dependencies without explicit approval.

## Layout

The repository has no application code yet. When the Android project is added,
keep feature code grouped by capability and preserve the layer boundaries in
[architecture.md](architecture.md):

```text
app/                 Android application module
docs/                Documentation
```

Do not create empty Gradle modules in advance. See Q-017.

## Code rules

- Follow the official Kotlin style and the existing style of the file you edit.
- Prefer immutable data and explicit state.
- UI reads state and sends events; it does not call DAOs.
- Domain code must not import Android framework classes.
- Repository interfaces live in the domain layer; implementations live in data.
- Model expected failures explicitly; do not swallow them.
- Never block the main thread with database or file work.
- Keep comments minimal and explain why, not what.
- No secrets, tokens, real financial data or machine-specific paths.
- Add a dependency only with approval; record a significant choice in the
  decision log.

## Git

- Default branch: `main`.
- Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
- One logical change per commit. Do not mix formatting, refactoring and behavior.
- Never commit build outputs, databases, exports, keystores or `.env` files.
- Keep the history clean; avoid noise commits during scaffolding.

## Testing

The test strategy will be defined with the first code. The intended split:

| Level | Focus |
|---|---|
| Unit | Domain rules, money and date calculations, use cases |
| Integration | Repository behavior, migrations, import/export validation |
| UI | Critical Compose flows |
| Security | Encryption, key handling, authentication gating |

No test commands exist yet. Add verified commands to `AGENTS.md` when the build
exists; never document guessed commands.

## Documentation

- `docs/` is the only place for explanations.
- Update the owning document in the same change that alters the fact.
- Record significant, hard-to-reverse decisions as an ADR using
  [../templates/adr.md](../templates/adr.md), and register it in the
  [decision log](../decisions/README.md).
- Add unresolved choices to [../open-questions.md](../open-questions.md); do not
  leave them buried in code review.
- Specify non-trivial features with
  [../templates/feature-spec.md](../templates/feature-spec.md) before building
  them.

## Related

- [Architecture](architecture.md)
- [Security](security.md)
- [Decision log](../decisions/README.md)
- [Open questions](../open-questions.md)
