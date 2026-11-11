# Talex

Talex is a privacy-first personal finance application for Android. It keeps
accounts, income and expenses, budgets, savings goals, assets and liabilities,
and analytics in one local app, without mandatory accounts or external servers.

> **Status:** early development. Version 0.2.0 adds accounts, categories, income
> and expenses, transfers, splits, recurring transactions and a cash-side capital
> overview on the encrypted local database.

## What it does

Talex gives one person a complete view of their money: what they own, what they
owe, and how it changes over time. It works fully offline and stores everything
in one encrypted database that the user controls.

## Principles

| Principle | What it means |
|---|---|
| Local-first | Every core feature works fully offline. No account or server is required. |
| Privacy-first | Financial data belongs to the user and stays on the device. |
| Security | Data is encrypted at rest; the master passphrase is the root of trust and biometrics are a local convenience. |
| Data ownership | The user can back up, restore, export and migrate their data. |
| Extensibility | The architecture leaves room for desktop and web clients. |
| Portability | No device sync; an exported encrypted snapshot moves data between devices. |

## Documentation

`docs/` is the source of truth for product and technical decisions.

| Read | When |
|---|---|
| [docs/README.md](docs/README.md) | You are new to the project or need the documentation index. |
| [Product overview](docs/product/overview.md) | You need to understand the concept and scope. |
| [Architecture](docs/technical/architecture.md) | You are making structural decisions. |
| [Roadmap](docs/roadmap.md) | You are planning the next phase. |
| [Open questions](docs/open-questions.md) | You need to know what is still undecided. |
| [Publishing conventions](docs/publishing.md) | You change the README, changelog or a release. |

## Build and run

JDK 21 is required.

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verified test and lint commands are in [AGENTS.md](AGENTS.md).

## Repository layout

```text
.
├── AGENTS.md          Rules for AI coding agents and contributors
├── CHANGELOG.md       Notable changes
├── README.md          This file
├── .editorconfig      Editor defaults
├── .gitignore         Ignored build, secret and local files
├── app/               Android application module
├── gradle/            Version catalog and Gradle wrapper
└── docs/              Product and technical documentation
```

## Contributing

Read [AGENTS.md](AGENTS.md) and
[docs/technical/development.md](docs/technical/development.md) before changing
the repository. Record unresolved choices in
[docs/open-questions.md](docs/open-questions.md) instead of deciding them in
code or in a discussion that will be lost.

## License

Talex is free software licensed under the **GNU General Public License v3.0**.
See [LICENSE](LICENSE) for the full text. If you distribute a modified version,
keep it under the same license and preserve the notices.
