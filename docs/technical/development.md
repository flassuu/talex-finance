# Development Conventions

Read this when you write code, tests, commits or documentation in this
repository.

**Status:** partially established. Language and documentation rules are binding;
tooling and code style will be confirmed when the Android project is created.

## Language

- Code, identifiers, comments, commit messages and repository documentation are
  English.
- Vault notes are Russian. The session log is Russian.
- The app UI uses English as the base language, Ukrainian as a translation and
  German next; there is no Russian UI (D-026). Do not hard-code strings.

## Stack

Pinned in `gradle/libs.versions.toml` and `app/build.gradle.kts`:

| Concern | Choice |
|---|---|
| Language | Kotlin 2.2.20 |
| Application ID | `io.github.flassuu.talex` – [ADR-0001](../decisions/0001-application-id.md) |
| Min / compile / target SDK | 26 / 36 / 36 – [ADR-0002](../decisions/0002-android-baseline.md) |
| Build | Gradle 8.14 with Kotlin DSL, AGP 8.13.1 |
| UI | Jetpack Compose, Material 3 |
| Persistence | Room 2.8.5 |
| Encryption | SQLCipher for Android 4.20.0 |
| Key derivation | Argon2id via `argon2kt` 1.6.0 – [ADR-0007](../decisions/0007-argon2id-kdf.md) |
| Authentication | Master passphrase with biometric unlock – [ADR-0004](../decisions/0004-portable-encrypted-database.md) |
| Keys | Android Keystore (biometric convenience only) |
| Async | Coroutines / Flow |
| DI | Hilt 2.57.2 with KSP |

The build requires JDK 21; Gradle 8.14 does not run on the newer system JDK.
Do not add dependencies without explicit approval.

## Layout

One Gradle module, `:app` (ADR-0014). Code is grouped by layer and capability
under `io.github.flassuu.talex`, preserving the boundaries in
[architecture.md](architecture.md):

```text
app/src/main/java/io/github/flassuu/talex/
  core/       money, identifiers, result types
  data/       database, repositories, encryption, session
  domain/     models, use cases, repository interfaces
  feature/    setup, unlock, accounts, home
  ui/         theme, components, navigation
gradle/       version catalog and wrapper
docs/         Documentation
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

Tests are intentionally deferred past the first Phase 1 build; the intended split
remains:

| Level | Focus |
|---|---|
| Unit | Domain rules, money and date calculations, use cases |
| Integration | Repository behavior, migrations, import/export validation |
| UI | Critical Compose flows |
| Security | Encryption, key handling, authentication gating |

Verified commands are in `AGENTS.md`. Phase 1 covers unit tests for money,
identifiers, formatting and account validation, plus instrumentation tests for
opening the encrypted database, rejecting a wrong key, Argon2id determinism and
the opening entry.

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
