# ADR 0014 – Start with a single `:app` module

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-017 (resolved by this ADR)

## Context

The Android application does not exist yet. The architecture already defines
logical layers and package groups, but not whether they should be separate Gradle
modules from the first commit. Multi-module builds add configuration, build time
and navigation overhead; a single module is faster to start but can drift into
tight coupling if boundaries are not enforced.

The project is built by one person first, with a diploma and portfolio deadline.
The first release needs a working encrypted core, not a large build graph.

## Decision

Start with one Gradle module, `:app`.

- Organize code by package boundaries: `core`, `data`, `domain`, `feature`, `ui`.
- Keep the domain package free of Android imports.
- Do not create feature modules, a separate `:core` module or convention plugins
  for the first release.
- Split a boundary into a module only when it is stable, has a clear contract and
  the build cost is justified.
- A future split must be a refactor with no behavior change, not a prerequisite
  for a feature.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Multi-module from day one | More build configuration and slower iteration before any product value exists; boundaries are still moving. |
| One module with no package boundaries | Fastest to write, but couples UI, domain and data and makes a later split expensive. |
| Split by layer (`:domain`, `:data`, `:ui`) immediately | A reasonable later step, but it adds Gradle overhead before the contracts are proven. |
| Separate modules per feature | Too many modules for a one-person first release; feature boundaries are not stable yet. |

## Consequences

- **Positive:** the first build is simple; iteration is fast; package boundaries
  still express the architecture; a later split is possible without changing
  behavior.
- **Negative:** boundaries are enforced by convention, not the compiler; package
  coupling can creep in if reviews are careless; a later split will touch imports
  and build files.
- **Follow-up:** `docs/technical/architecture.md` records the package groups and
  points to this ADR; code review checks that `domain` has no Android imports;
  the module decision is revisited when build times or team size grow.
- **Resolved question:** Q-017 is closed as "one `:app` module with package
  boundaries".

## Review

Revisit when the build becomes slow, when a second developer joins, when a
boundary has been stable across several features, or when a second client needs to
reuse the domain layer. A split must be justified by a measured cost, not by
style alone.
