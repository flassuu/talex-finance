# Architecture

Read this when you are making a structural decision, adding a module, or
defining a boundary between UI, domain and storage.

**Status:** preliminary. No application code exists. The structure below is a
working direction and must be validated with a technical spike before it
hardens.

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

These choices are provisional. Replacing one requires a decision record, not a
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

Proposed package groups, subject to change:

```text
core/        shared primitives, result types, time and money helpers
data/        database, repositories, encryption, import/export
domain/      models, use cases, repository interfaces
feature/     one package per user-facing capability
ui/          theme, shared Compose components, navigation
```

## Future clients

The Android app is the first client, not the only possible one. To keep desktop
and web viable:

- Keep domain models and use cases free of Android types.
- Define repository contracts around domain objects, not database rows.
- Keep platform-specific concerns — keystore, biometrics, file access — behind
  interfaces in the data or platform layer.
- Do not design a synchronization protocol now. When sync is considered, it
  enters behind an explicit adapter boundary (Q-016).

## Error handling

- Model expected failures explicitly instead of throwing across layers.
- Keep user-facing messages in the UI layer; domain returns outcomes.
- Never log financial values, account identifiers or encryption material.

## Open questions

The platform baseline is recorded in ADR-0001 and ADR-0002, the portable
storage model in ADR-0004, identifiers and key derivation in ADR-0006 and
ADR-0007, and the single-module start in ADR-0014. Synchronization remains
undecided. See Q-016 in [../open-questions.md](../open-questions.md).

## Related

- [Database](database.md)
- [Ledger](ledger.md)
- [Storage, portability and sync](storage-and-sync.md)
- [Security](security.md)
- [Development conventions](development.md)
- [Decision log](../decisions/README.md)
- [Roadmap](../roadmap.md)
