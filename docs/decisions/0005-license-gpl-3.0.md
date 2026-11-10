# ADR 0005 – License: GNU GPL v3.0

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-015

## Context

Talex is free and open source (D-015). The specific license determines whether
derivatives must remain open, how the project can be reused, and what a future
employer or contributor may do with the code. The project is also a diploma and
portfolio piece.

## Decision

License Talex under the **GNU General Public License v3.0**. Ship the full text
in `LICENSE` and keep the source available.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Apache-2.0 | Permissive and employer-friendly, but allows proprietary forks of a privacy-focused app. |
| MIT | Simplest permissive license, with the same concern about closed derivatives. |
| No license | Removes the freedom to use and share the code and is not open source. |

## Consequences

- **Positive:** derivatives stay open; the privacy commitment is reflected in the
  license; the project is clearly open source.
- **Negative:** some companies avoid GPL dependencies; a proprietary fork is not
  possible.
- **Follow-up:** check that every dependency is GPL-3.0 compatible. Do not add a
  license header or change the license without a new ADR.

## Review

Revisit only if the distribution model changes or a dependency cannot be used
under GPL-3.0.
