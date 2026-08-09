# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Centralized own version and pinned upstream dependency versions (`qilletni-core`,
  `qilletni-pkgutil`) in `gradle.properties`, and a `.qilletni/release.yml` matching the
  central Qilletni/Qilletni release schema, onboarding this repository onto the shared
  release-preparation, dependency-update, and platform-release-dispatch automation.
  See `RELEASE.md` for the updated release process and `docs/migrations/README.md`.
- Dependency locking (`gradle.lockfile`), a stable-dependency guard
  (`checkNoSnapshotDependencies`), and a CycloneDX JSON SBOM, generated for every build.
- `component-manifest.json`, packaged inside the release archive (and attached separately
  as a release asset) alongside the SBOM, recording this build's own version, the exact
  `qilletni-api`/`qilletni-pkgutil` versions it was built against, and the source commit.
- `qpm --version` now also reports the embedded `qilletni-api`/`qilletni-pkgutil` versions
  and source commit it was built against.
- A `repository_dispatch` caller for the central reusable dependency-update workflow
  (never auto-merges), and a `qilletni-platform-component-release` dispatch to
  Qilletni/Qilletni after every stable release.

### Changed

- Reworked the release workflow to a marker-based, automatic, immutable-tag flow:
  merging a release-preparation PR now tags and publishes the release itself, with
  idempotent recovery and an automatic next-patch snapshot PR. See `RELEASE.md`.

## [1.0.0] - 2025-11-01

### Added

- Initial QPM implementation
