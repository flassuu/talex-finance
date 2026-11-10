# ADR 0015 — No synchronization; export/import portability

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner
- **Related questions:** Q-016

## Context

The earlier direction (ADR-0004, D-023) anticipated file-level synchronization
between devices. The owner has since decided that synchronization is not needed:
an exported snapshot of the database is sufficient, and the database can simply
live in the system's app storage. This removes a whole class of complexity and
risk.

## Decision

- Do **not** implement device synchronization, now or later as a planned
  feature. Remove it from the product direction.
- Portability is **export and import** of the encrypted database snapshot,
  written to and read from a user-chosen location.
- The working database lives in the app's internal storage (D-036); it is not
  visible to other apps.
- There is no Android system or cloud backup (D-037); backups are manual exports
  with a reminder (D-038).

This supersedes D-023 and the synchronization clause of ADR-0004.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| File-level sync (Syncthing/self-hosted) | Adds conflict handling and network surface the owner does not want. |
| Server-side sync | Violates local-first and privacy-first principles. |
| Android system backup | Places financial data in cloud storage; the owner chose explicit local export. |

## Consequences

- **Positive:** no conflict resolution, no sync metadata, fewer attack surfaces;
  the storage model is simpler to build, test and secure.
- **Negative:** moving between devices is a manual export/import, and the
  database is tied to the current install until exported.
- **Follow-up:** ensure the export/import flow is tested end to end on a clean
  install and another device, and that soft deletion and UUIDv7 identifiers
  remain for audit and import stability, not for sync.

## Review

Revisit only if multi-device use becomes a stated requirement; that would be a
new decision, not a revival of the superseded sync direction.
