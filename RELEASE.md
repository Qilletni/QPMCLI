## QPM Release Process

QPM is one of five repositories onboarded as release producers in the Qilletni ecosystem
(see `RELEASE.md` in `Qilletni/Qilletni`). It declares its own `.qilletni/release.yml`
(schema/validation in `Qilletni/Qilletni`'s `tools/release/src/release_config.ts`). The
central, reusable release-preparation/publish logic lives in `Qilletni/Qilletni`
(`.github/workflows/reusable-*.yml`); this repository's own `.github/workflows/` only
contains small local "caller" workflows that invoke it.

### Version centralization

QPM's own version (`qpmVersion`), and the pinned versions of the two upstream components it
consumes, are all declared once in the root `gradle.properties`:

- `qilletniCoreVersion` - pins both `dev.qilletni.impl:qilletni` (core) and
  `dev.qilletni.api:qilletni-api`, which `Qilletni/Qilletni` always releases together at the
  same version as one atomic unit registered as `qilletni-core`. QPM only ever adds
  `qilletni-api` as an actual build dependency (never core) - in `.qilletni/release.yml`,
  the core coordinate is marked `resolved: false`, so it stays mandatory/hash-verified and
  coupled to this same version, without ever being expected on QPM's own classpath.
- `qilletniPkgutilVersion` - pins `dev.qilletni.pkgutil:qilletni-pkgutil`.

### Preparing a release

1. Run the `Release - Prepare` workflow (`workflow_dispatch`), choosing a
   `patch`/`minor`/`major` bump.
2. It calls `Qilletni/Qilletni/.github/workflows/reusable-release-prepare.yml@master`, which
   computes the next version from the latest stable `vX.Y.Z` tag, requires a non-empty
   `## [Unreleased]` section in `CHANGELOG.md` (and, for a `major` bump, a
   `docs/migrations/X.Y.Z.md` guide), runs `./gradlew clean test`, updates `qpmVersion`,
   promotes the changelog, writes a `release/pending-release.json` marker, and opens a
   signed, review-only PR.
3. **This PR never auto-merges**; a maintainer reviews and merges it manually.

### Publishing a release (fully automatic after merge)

`release.yml` reacts to `master` pushes and to `vX.Y.Z` tag pushes:

- **`tag-release`** (every push to `master`): if `qpmVersion` is still a `-SNAPSHOT`, this is
  an ordinary commit and nothing is tagged. Otherwise it must be the merge of a
  release-preparation PR - the `release/pending-release.json` marker and the merge commit's
  originating PR are both validated - before **idempotently** creating (or, if it already
  exists at this exact commit, no-oping on) the immutable `vX.Y.Z` tag. A direct tag push
  remains supported only as a manual recovery path, and is likewise idempotent.
- **`publish-snapshot`** (every push to `master`, its own job, isolated from
  `build-and-publish` - no `production-release` environment): whenever the version is still a
  `-SNAPSHOT`, builds the release archive and re-publishes it under the floating `snapshot`
  tag as a prerelease.
- **`build-and-publish`** (triggered only by the `vX.Y.Z` tag push, inside the protected
  `production-release` GitHub Environment): validates the tag against `qpmVersion`, checks
  the resolved dependency graph has no SNAPSHOT/dynamic versions
  (`checkNoSnapshotDependencies`), builds the release archive (`releaseArchive`: the shadow
  jar as `QPM.jar`, the `qpm`/`qpm.bat` launcher scripts, the CycloneDX JSON SBOM, and
  `component-manifest.json`), and creates the GitHub Release with all of it attached
  (archive, raw jar, SBOM, manifest).

  QPM is a CLI distribution, not a published Maven library, so it has no japicmp public-API
  gate here (see `docs/migrations/README.md`); its own compatibility is instead enforced by
  the PR CI workflow's build/test/archive/manifest gates.
- **`dispatch-platform`** (after `build-and-publish`): sends the exact
  `qilletni-platform-component-release` event (`component: qpm`, with the release archive's
  asset name/sha256, the tagged commit, and the embedded `api`/`pkgutil` versions) to
  `Qilletni/Qilletni`, via a GitHub App installation token scoped to *only*
  `Qilletni/Qilletni`.
- **`snapshot-followup`** (after `dispatch-platform`): opens a follow-up PR bumping
  `qpmVersion` to `X.Y.(Z+1)-SNAPSHOT` and removing the consumed release marker, so `master`
  immediately resumes snapshot publishing. Never auto-merges.

### Receiving upstream dependency updates

`dependency-update.yml` receives a `qilletni-dependency-release` dispatch (sent by
`Qilletni/Qilletni` for `qilletni-core`, or `QilletniPackageUtility` for `qilletni-pkgutil`)
and forwards it, with this repository's own scoped App credentials, to the central
`reusable-dependency-update.yml`, which:

1. Validates the payload's `component`/`repository`/coordinate-set/version against this
   repository's own `.qilletni/release.yml` `dependencies` mapping.
2. Re-downloads and hash-verifies every artifact against Maven Central.
3. Applies the update to only the one configured `qilletniCoreVersion`/`qilletniPkgutilVersion`
   property (idempotent), refreshes Gradle dependency locks (sibling composite builds
   explicitly disabled), runs the full test suite plus `checkNoSnapshotDependencies`, and
   confirms the resolved dependency graph actually contains the requested version(s) - the
   `qilletni-core` coordinate marked `resolved: false` is excluded from that last check, since
   it is never expected to actually be on QPM's classpath.
4. Opens a signed, review-only PR. **This PR never auto-merges.**

### Sibling composite builds

Local development against unreleased sibling checkouts (`../Qilletni`,
`../QilletniPackageUtility`) remains available, but only when explicitly opted into via
`-PincludeSiblingBuilds=true` - release, CI, and dependency-update builds always resolve the
pinned, published Maven coordinates instead.

### Authentication

Every cross-repository dispatch and every PR this automation opens authenticate as a GitHub
App (organization secrets `QILLETNI_RELEASE_APP_ID` / `QILLETNI_RELEASE_APP_PRIVATE_KEY`),
each token scoped to exactly one target repository - never a broad, org-wide token.

See `Qilletni/Qilletni`'s own `RELEASE.md` for the full ecosystem-wide picture, including
platform (Docker) releases.
