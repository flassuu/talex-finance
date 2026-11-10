# ADR 0007 – Argon2id key derivation

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-019

## Context

The database is a portable encrypted file. An attacker who copies it can guess
the master passphrase offline. A fast hash or a weak KDF makes that attack cheap.
The KDF must be memory-hard, available in the Android stack, and adjustable as
devices get faster.

## Decision

Derive the database key from the master passphrase with **Argon2id**. Tune the
memory, time and parallelism parameters on target devices, record them with the
database version, and benchmark unlock time. Use a maintained Argon2
implementation; do not write the primitive from scratch.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| PBKDF2 | Widely available, but not memory-hard; weaker against GPU and ASIC attacks. |
| scrypt | Memory-hard, but less standardised for this use and with fewer maintained Android options. |
| SQLCipher's built-in KDF only | Convenient, but the default is not memory-hard enough for a portable file. |

## Consequences

- **Positive:** offline guessing becomes significantly more expensive.
- **Negative:** unlock takes measurable time and memory; the parameters must be
  versioned so a future change can rekey existing files.
- **Follow-up:** verify the library on API 26 devices, record parameters in the
  storage format, and test that changing parameters does not break restore.

## Review

Revisit after target-device benchmarks, or if a maintained Android Argon2
implementation is unavailable or unacceptably slow.
