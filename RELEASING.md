# Releasing the Blaaiz Java SDK

This document explains how a release reaches Maven Central. It is for maintainers.

## How a release works

1. Merge pull requests to `main` with Conventional Commit messages. A `feat:` commit starts a
   minor release. A `fix:` commit starts a patch release. A `feat!:` commit, or a commit with a
   `BREAKING CHANGE:` footer, starts a major release. Only `feat`, `fix`, `perf`, and `revert`
   commits start a release. Other types, such as `docs:`, `test:`, `chore:`, and `ci:`, do not.
   release-please ignores a commit message that is not a Conventional Commit.
2. release-please opens or updates a pull request titled `chore(release): release X.Y.Z`. This
   pull request bumps every version string in the SDK and adds an entry to `CHANGELOG.md`.
3. Review the release pull request. Make sure that it changes only version strings and
   `CHANGELOG.md`, and that the latest CI run on `main` passed. Then merge it.
4. The workflow then creates tag `vX.Y.Z`, creates the GitHub Release, and runs the `publish`
   job in the same workflow run.
5. The `publish` job publishes the new version to Maven Central.

## Rules

- Do not change the version by hand.
- Do not create a tag or a GitHub Release by hand.
- Merge a pull request with **Create a merge commit**, and give it a plain title that is not a
  Conventional Commit. GitHub copies the title into the merge commit, and release-please reads a
  Conventional Commit title there as an extra changelog line.
- Do not squash-merge a pull request that has a plain title. release-please then ignores all of its
  changes.

## Keep the SDKs on one version

The Blaaiz SDKs for Java, Node.js, Laravel/PHP, and Python use the same version number for the
same set of features. To set a specific version for a release, add a `Release-As: X.Y.Z` footer
to a commit on `main`.

## Files that contain the version

release-please's Maven release type updates the `<version>` tag of every `pom.xml` file in the
repository on its own: `pom.xml` and `examples/pom.xml`.

It also updates these files, because they hold the version outside a native `<version>` tag:

- `README.md` — the Maven and Gradle install snippets
- `src/main/java/com/blaaiz/sdk/BlaaizClient.java` — the `USER_AGENT` constant
- `src/test/java/com/blaaiz/sdk/BlaaizClientTest.java` — the two assertions on the `User-Agent`
  header
- `examples/pom.xml` — the `<version>` of the `blaaiz-java-sdk` dependency (the module's own
  `<version>` tag is handled natively; this second `<version>` tag, inside the `<dependency>`
  block, needs the marker comment because it is not the module's own version)

The first release pull request can also reformat the `<project>` element of each `pom.xml` onto
one line. This change is cosmetic.

Do not edit `CHANGELOG.md` by hand. release-please owns this file from the first automated
release onward.

## Note on the release pull request

GitHub holds the CI runs of a pull request that GitHub Actions opens. To run CI on the release PR,
select **Approve and run** on it. The `publish` job does not run on a pull request. The release PR
changes only version strings, `.release-please-manifest.json`, and `CHANGELOG.md`. After you merge
it, CI runs on `main`, and the `publish` job waits for the tests to pass first.

## Required setup

1. Go to **Settings > Actions > General > Workflow permissions**.
2. Select **Allow GitHub Actions to create and approve pull requests**.

The `publish` job reads these repository secrets:

| Secret                    | Purpose                                    |
| -------------------------- | ------------------------------------------ |
| `MAVEN_CENTRAL_USERNAME`   | The Maven Central Portal username           |
| `MAVEN_CENTRAL_PASSWORD`   | The Maven Central Portal password           |
| `GPG_PRIVATE_KEY`          | The GPG key that signs the release artifacts |
| `MAVEN_GPG_PASSPHRASE`     | The passphrase for the GPG key              |

## If a job fails after the merge

release-please can create the tag and the GitHub Release in the same workflow run in which a test
fails. The `publish` job then does not run.

- If the failure is a flaky test or an outside problem, open the workflow run that created the
  release. Select **Re-run failed jobs**. The `publish` job then publishes the tagged commit.
- If the tagged code is broken, merge a fix. release-please then prepares the next patch release.

## Manual fallback

You can still publish a GitHub Release for an existing tag by hand. The `publish` job also runs
for that event, so this path stays available if release-please is unavailable.
