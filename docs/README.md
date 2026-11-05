# Talex Documentation

Read this when you need to find the document that owns a fact. `docs/` is the
source of truth for product and technical explanations; `AGENTS.md` holds rules
and commands.

## Start here

| If you need to... | Read |
|---|---|
| Understand the product and its scope | [product/overview.md](product/overview.md) |
| Understand the full plan, functional areas and MVP | [product/plan.md](product/plan.md) |
| Plan or evaluate market research | [product/market-research.md](product/market-research.md) |
| Make a structural or code-organization decision | [technical/architecture.md](technical/architecture.md) |
| Model data or write a migration | [technical/database.md](technical/database.md) |
| Touch storage, authentication, backups or logging | [technical/security.md](technical/security.md) |
| Write code, commits or documentation | [technical/development.md](technical/development.md) |
| Plan the next phase | [roadmap.md](roadmap.md) |
| See what has been decided | [decisions/README.md](decisions/README.md) |
| See what is not decided | [open-questions.md](open-questions.md) |
| Record a decision or specify a feature | [templates/](templates/) |

## Reading order for a new agent

1. [product/overview.md](product/overview.md)
2. [product/plan.md](product/plan.md)
3. [technical/architecture.md](technical/architecture.md)
4. [technical/security.md](technical/security.md)
5. [open-questions.md](open-questions.md)
6. The document for the task at hand.

## Status labels

| Label | Meaning |
|---|---|
| **Established** | Decided and binding until superseded by an ADR. |
| **Provisional** | A working choice that may change; do not build irreversible dependencies on it. |
| **Open** | Not decided. Tracked in `open-questions.md`. |

Every document states its status near the top. When a document mixes categories,
it labels the sections.

## Rules

- English only, including headings.
- Start with a purpose line and a "Read this when" section.
- One fact lives in one file; other documents link to it.
- Do not restate code. Name the file and what to look for.
- Update the owning document in the same change that alters the fact.
- Do not treat this documentation as a substitute for a security review.
