# Migration Guides

QPM is a CLI distribution (an executable archive), not a published Maven library, so it has
no `japicmp` public-API compatibility gate (see `.qilletni/release.yml`, `kind: cli`, and
`RELEASE.md`). Its own compatibility is instead enforced by the PR CI workflow's build,
test, `shadowJar`, release-archive, dependency-lock, stable-dependency-guard, and
`component-manifest.json`/SBOM-inspection gates - never by a fabricated public-API baseline.

A major release (`X.0.0`) that introduces a breaking change to QPM's own command-line
interface or its on-disk/network integration behavior (subcommands, flags, exit codes,
config file format, package archive format, registry protocol, etc.) requires a migration
guide at `docs/migrations/X.Y.Z.md` (named after the new version being released).

Each guide should describe, for every breaking change:

- What changed and why.
- The affected CLI surface (subcommand, flag, exit code, file/protocol format).
- How users/scripts invoking `qpm` should migrate.
