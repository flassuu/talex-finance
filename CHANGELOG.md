# Changelog

All notable changes to Talex are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning will follow
[Semantic Versioning](https://semver.org/) once releases begin.

## [Unreleased]

### Added

- Initial repository structure: `README.md`, `AGENTS.md`, `CHANGELOG.md`,
  `.editorconfig` and `.gitignore`.
- Documentation foundation under `docs/`: product overview, market research
  framework, architecture, database, security, development conventions, roadmap,
  decision log, open-question register and templates.
- Recorded initial decisions: application ID `io.github.flassuu.talex`, minSdk
  26, and the portable encrypted database model with a master passphrase.
- Added the product plan (`docs/product/plan.md`): vision, capital-dashboard
  concept, six functional areas, MVP direction, positioning hypotheses and
  competitor candidates. Expanded the open-question register (Q-020 … Q-024).
- Adopted a portable encrypted database model (ADR-0004), single-entry ledger,
  unified account model, English/Ukrainian localization, and recorded the new
  decisions D-020 … D-026.
- Selected GPL-3.0 and added `LICENSE` (ADR-0005); recorded basic analytics,
  single-user profiles and decisions D-027 … D-029.
- Finalized exchange-rate, backup and recovery defaults (D-030 … D-032).
- Selected UUIDv7 identifiers (ADR-0006) and Argon2id key derivation
  (ADR-0007); decisions D-033 … D-034.
- Removed device synchronization: portability is export/import of the encrypted
  snapshot; the database lives in internal storage with no system/cloud backup
  (ADR-0015; decisions D-035 … D-038).
- Defined the UX direction (`docs/product/ux.md`): configurable bottom bar with a
  center quick-add and a trailing FAB menu, Material 3 with dynamic color, a
  wizard onboarding with a demo profile, and the capital-card overview
  (D-039 … D-043).
- Resolved the capital formulas (ADR-0016), goals independence (ADR-0017) and
  liabilities as negative-balance accounts (ADR-0018), and finalized the feature
  scope (kept savings rate, cash flow, asset dynamics, crypto and ROI; dropped
  forecast, alerts and bank CSV profiles).
