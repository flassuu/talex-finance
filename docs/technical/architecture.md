---
modified: 2026-11-10T22:53:47
---
# Architecture

Read this when you are making a structural decision, adding a module, or
defining a boundary between UI, domain and storage.

**Status:** partially implemented. Phase 1 builds the single-module Android
skeleton with the layers below; the Phase 1 vertical slice proves the storage and
authentication path. Exact versions live in
[development.md](development.md).

## Drivers

- Fully offline operation with no mandatory server.
- Encryption and authentication at the storage boundary.
- A domain model that can outlive the Android client.
- Testable business rules independent of Android frameworks.
- Incremental delivery: a small working core before broad feature coverage.

## Proposed shape

A single Android application organized by feature and split into three logical
layers:

```text
Compose UI + ViewModel
        │
        ▼
Domain: models, use cases, repository interfaces
        │
        ▼
Data: repository implementations, Room DAOs, SQLCipher, Keystore access
        │
        ▼
Encrypted local database
```

| Layer | Responsibility | Must not |
|---|---|---|
| UI | Render state, collect user intent, navigation | Query the database directly |
| Domain | Business rules, calculations, contracts | Import Android framework types |
| Data | Persistence, encryption, mapping, migrations | Contain product rules that belong in domain |

State flows downward; events flow upward. Coroutines and `Flow` carry
asynchronous work; no blocking database access on the main thread.

## Technology direction

These choices are pinned in the build; versions are recorded in
[development.md](development.md). Replacing one requires a decision record, not a
silent change.

| Concern | Current direction |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Presentation | ViewModel with immutable UI state |
| DI | Hilt |
| Async | Coroutines and Flow |
| Persistence | Room over SQLite |
| Encryption | SQLCipher |
| Authentication | Master passphrase; AndroidX Biometric as a local convenience (ADR-0004) |
| Keys | Android Keystore |

## Module strategy

Start with a single `:app` Gradle module and enforce package boundaries by
convention. Extract Gradle modules only when a boundary is stable and the build
cost justifies it (ADR-0014).

Package groups under `io.github.flassuu.talex`, implemented in Phase 1:

```text
core/        money, UUIDv7 identifiers, result types
data/        Room database, repositories, SQLCipher, Argon2id, session
domain/      models, use cases, repository interfaces
feature/     one package per user-facing capability (setup, unlock, accounts, home)
ui/          theme, shared Compose components, navigation
```

The `domain` package imports no Android framework types.

## Future clients

The Android app is the first client, not the only possible one. To keep desktop
and web viable:

- Keep domain models and use cases free of Android types.
- Define repository contracts around domain objects, not database rows.
- Keep platform-specific concerns – keystore, biometrics, file access – behind
  interfaces in the data or platform layer.
- There is no device synchronization (D-035). A future client reuses the file
  format through export and import, not a live sync.

## Error handling

- Model expected failures explicitly instead of throwing across layers.
- Keep user-facing messages in the UI layer; domain returns outcomes.
- Never log financial values, account identifiers or encryption material.

## Open questions

The platform baseline is recorded in ADR-0001 and ADR-0002, the portable
storage model in ADR-0004, identifiers and key derivation in ADR-0006 and
ADR-0007, the single-module start in ADR-0014, and the no-synchronization
direction in ADR-0015. See [../open-questions.md](../open-questions.md) for the
remaining open items.
