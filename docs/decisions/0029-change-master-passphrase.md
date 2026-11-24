# ADR 0029 – Changing the master passphrase

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-036; ADR-0004, ADR-0007, ADR-0019, ADR-0028

## Context

The master passphrase is the root of trust for the portable file (ADR-0004). The
database key is Argon2id over the passphrase with the salt and parameters stored
in `talex.db.meta` (ADR-0019), and SQLCipher applies its own per-file salt on top
of that key. `technical/security.md` requires that the user can change the
passphrase, but the current build can only create it.

A change touches two artifacts that must stay consistent: the encrypted database
and its KDF sidecar. A crash between rewriting one and the other would leave a
file that neither the old nor the new passphrase opens. The change must also
invalidate the Keystore-wrapped biometric copy of the old key (ADR-0004).

## Decision

Changing the passphrase **rewrites the database with the new key into a
temporary file and swaps it in**, instead of re-keying the live file in place:

1. Derive the key from the entered current passphrase and compare it with the
   in-memory key. A mismatch is rejected before anything is written.
2. Generate a new salt, new `KdfMetadata` and the new key.
3. Checkpoint the WAL, then use `ATTACH DATABASE` + `sqlcipher_export` to write a
   complete copy to `talex.db.rekey`, and re-key that copy with the new key.
4. Close the database. Save the old sidecar as `talex.db.meta.old`, move the old
   database to `talex.db.bak`, install the new file and write the new sidecar.
5. Delete `talex.db.bak` and `talex.db.meta.old`, reopen with the new key, and
   replace the wrapped biometric key: clear it, then re-wrap the new key when
   biometric unlock was enabled.

The two marker files make the operation recoverable. On startup, when
`talex.db.meta.old` exists, the app compares it with the current sidecar: an
identical sidecar means the new metadata was not written, so the backup database
is restored; a different sidecar means the new metadata is in place, so the
backup is discarded.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| `PRAGMA rekey` on the live database | Rewrites pages in place; a crash on the way leaves a file that no passphrase opens. |
| Keep the old salt and derive a new key with it | Reusing the salt weakens the KDF output and does not hide that the passphrase changed. |
| No passphrase change in the product | `technical/security.md` treats the passphrase as a user-owned secret; leaving it unchangeable is a security gap. |
| Journal both metadata blobs in one file | Equivalent recovery value with more code; the old sidecar plus the file naming already identifies the state. |

## Consequences

- **Positive:** the passphrase can be changed without exporting and re-importing a
  backup, and the biometric key never outlives the passphrase it was wrapped for.
- **Negative:** the change needs free space for a second copy of the database and
  takes as long as an Argon2id derivation plus a full copy.
- **Follow-up:** `technical/security.md` and `technical/storage-and-portability.md`
  describe the markers; a test proves that a changed passphrase opens the file
  and the old one does not.

## Review

Revisit when the KDF parameters become upgradable, when a PIN quick-unlock is
introduced, or when the file format gains a built-in journal.
