# Talex

Talex is a privacy-first personal finance application for Android. It brings
accounts, income and expenses, budgets, savings goals, assets and liabilities,
and financial analytics into one local application — without mandatory accounts
or external servers.

> **Status:** documentation foundation. The repository does not contain
> application code, a Gradle build, or a finalized database schema yet.

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

## Repository layout

```text
.
├── AGENTS.md          Rules for AI coding agents and contributors
├── CHANGELOG.md       Notable changes
├── README.md          This file
├── .editorconfig      Editor defaults
├── .gitignore         Ignored build, secret and local files
└── docs/              Product and technical documentation
```

Application sources will be added when the technical foundation is approved.
The proposed stack is documented in
[docs/technical/architecture.md](docs/technical/architecture.md); it is
preliminary.

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
