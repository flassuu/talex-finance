---
description: Publish to GitHub: remote, README, changelog and a clean release
---

Publish this repository to GitHub following docs/publishing.md. Work end to end
and stop only for a decision or a missing secret.

First read docs/publishing.md and follow it exactly.

1. Check the state: `git remote -v`, `gh auth status`, the `versionName` in
   app/build.gradle.kts, and whether a release keystore is configured.
2. Polish README.md: status line, one-line description, verified commands, docs
   links and license. No emojis, no screenshots.
3. Make sure CHANGELOG.md has an `[Unreleased]` section and a dated section that
   matches `versionName`. One sentence per bullet, written for users.
4. Create the GitHub repository with `gh repo create` if no remote exists. Use
   $ARGUMENTS as the repository name when given, otherwise ask.
5. Build the release APK. If signing is not configured, report it instead of
   publishing a debug build.
6. Write the release notes as a short, user-facing copy of the changelog entry.
7. Tag `v<version>`, push `main` and the tag, and create the GitHub release with
   the APK attached.
8. Report the release URL and anything still open.
