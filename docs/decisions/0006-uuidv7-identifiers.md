# ADR 0006 – UUIDv7 identifiers

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-005

## Context

Entity identifiers must survive export, import and future synchronization. They
must not depend on a local auto-increment sequence, which collides when data from
two devices is merged. Sorting by creation time is useful for lists and change
tracking. A portable database is opened on more than one device, so identifiers
must be generated without a central authority.

## Decision

Use **UUIDv7** (time-ordered UUIDs) for persistent entities. Store them as text
in SQLite and expose them as strings in backups and future sync payloads.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Auto-increment integers | Compact, but collide on merge and are hard to reconcile across devices. |
| UUIDv4 | Stable and random, but not sortable by creation time. |
| ULID | Similar properties, but UUIDv7 is a standard format with broader tooling. |

## Consequences

- **Positive:** IDs are globally unique, mergeable and roughly sortable.
- **Negative:** UUIDv7 embeds a creation timestamp; this is acceptable because
  transactions already carry dates, but it should not be treated as a secret.
- **Follow-up:** use a tested UUIDv7 generator (library or small utility) and
  add a uniqueness test. Do not expose database row numbers as public IDs.

## Review

Revisit if a sync design requires a different identifier or if the timestamp
component becomes a privacy concern.
