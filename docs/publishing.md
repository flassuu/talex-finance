---
modified: 2026-11-10T22:45:06
---
# Publishing Conventions

Read this when you write or change the README, a changelog entry, a GitHub
release, a store listing, or a version number. This file is the single source of
truth for how Talex presents itself to the public.

**Status:** established. These rules apply to every public-facing text. They are
a working guide: refine them here, not in a single file.

## Channels

One fact can appear in several places, but each place has a different job.

| Channel | Audience | Length |
|---|---|---|
| `README.md` | Anyone who opens the repository | About one screen |
| `CHANGELOG.md` | Users and contributors over time | Lean list, one line per change |
| GitHub release | Users of one specific version | A few short bullets |
| Store "What's new" | Store users | Two or three sentences |

`CHANGELOG.md` is the source of truth. GitHub release notes and store text are
shortened copies of it, never new facts.

## Non-negotiable rules

These apply to everything in the table above.

1. No emojis. Write the word instead.
2. Use the en dash `–` for dashes in prose, as in the vault notes. Do not use the
   em dash (`U+2014`).
3. No screenshots for now. Add them once the UI is stable and a real release
   exists; until then, describe features in words.
4. Plain, human English. Short sentences, active voice, second person where it
   helps. Write as you would explain to a colleague.
5. One idea per line. A bullet or a sentence says one thing.
6. Benefit first. State what changed for the user, then the detail if it matters.
7. No walls of text. If a section grows past a screen, it needs its own document
   in `docs/`.

## README.md

The README is the front page. Keep it short and link to `docs/` for depth.

Order of sections, include only what exists:

1. Title and a one-line description.
2. Status line: early development, released, archived.
3. What the app does, in two or three sentences.
4. Principles or features as a short table or list.
5. Screenshots, once the UI is stable.
6. Documentation links, pointing at `docs/README.md`.
7. Build and run, copy-paste commands from `AGENTS.md`.
8. Repository layout.
9. Contributing, a link to `AGENTS.md`.
10. License.

Rules:

- Do not duplicate what `docs/` explains. Link to the owning file instead.
- Keep commands verified and copy-paste ready.
- Update the README in the same change that changes behavior.

## Versioning

Follow Semantic Versioning.

- During initial development use `0.y.z`: bump the minor for each milestone, the
  patch for bug fixes.
- `1.0.0` marks a stable public API and the first real release.
- Tag releases as `v0.1.0`, `v0.2.0`. The tag carries the `v` prefix; the
  `versionName` in `app/build.gradle.kts` does not, and the two must match.
- A pulled release is marked `[YANKED]` in the changelog and on the release page.

## CHANGELOG.md

Follow Keep a Changelog. The structure is:

```
# Changelog

All notable changes to Talex are documented here. The format follows
Keep a Changelog, and versioning follows Semantic Versioning.

## [Unreleased]

## [0.2.0] - 2026-11-10

### Added

- One change, one sentence.

### Fixed

- One change, one sentence.
```

Sections, in this order, and only the ones that have entries: Added, Changed,
Deprecated, Removed, Fixed, Security.

Rules:

- One bullet per notable change, one sentence each.
- Write the user-facing outcome, not the commit message. "The app now re-locks
  after five minutes", not "changed the re-lock policy constant".
- Newest version first, each with an ISO date (`YYYY-MM-DD`).
- Keep `[Unreleased]` at the top; move its entries into a dated section when you
  tag.
- Do not paste `git log`. Curate.
- Never edit a released section except to fix an error. A pulled release is
  marked, not deleted.
- Stay lean. This is a running list of notable changes, not every commit; a full
  history should still read in a few minutes.

## GitHub releases

A release is the storefront for one version. Keep it short and user-facing.

```
## 0.2.0

Accounts can be edited and archived, and you can record income and expenses.

### Added

- Edit and archive accounts.
- Record income and expense entries with categories and tags.

### Fixed

- The app re-locks after five minutes in the background.
```

Rules:

- Lead with the most important change.
- One sentence per bullet, plain language. Issue numbers only when they help the
  reader.
- Put breaking changes and data migrations first, and call them out.
- Attach the signed APK as a release asset. Never commit the APK.
- The release notes are a trimmed, user-facing copy of the matching changelog
  entry.

## Store "What's new"

The store shows a couple of lines. Write two or three sentences at most, one per
point, focused on what the user can now do.

- Use the language of the listing: English as the base, Ukrainian for the
  Ukrainian listing.
- Do not mention internals, refactors or dependency bumps.
- If a change affects data or needs a user action, say so plainly.

Example:

> Accounts can now be edited and archived. You can record income and expense
> entries with categories and tags. The app re-locks after five minutes in the
> background.

## Checklist

Run this before publishing any public text:

- [ ] No emojis.
- [ ] No em dash. Use "–" for text and "-" for numbers. 
- [ ] No screenshots unless the UI is stable and the release is real.
- [ ] Each bullet is one sentence.
- [ ] Breaking changes and migrations are first and explicit.
- [ ] Version and date match the tag.
- [ ] The changelog entry exists before the release notes are written.

## Related

- [Development conventions](technical/development.md)
- [Roadmap](roadmap.md)
- [Documentation index](README.md)
- [Decision log](decisions/README.md)
