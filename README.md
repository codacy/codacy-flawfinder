# Codacy Flawfinder

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d8e01051a03945e7bb83785b5cbd904e)](https://www.codacy.com/gh/codacy/codacy-flawfinder?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-flawfinder&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-flawfinder.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-flawfinder)

This is the docker engine we use at Codacy to have [Flawfinder](https://dwheeler.com/flawfinder/) support.
You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

## Usage

You can create the docker by doing:

```bash
make all
sbt docker:publishLocal
```

The docker image can be used with the following command:

```bash
docker run -it -v $srcDir:/src <DOCKER_NAME>:<DOCKER_VERSION>
```

#### Generate Docs

1. Run the script, from the project root, to generate documentation:

```bash
make all
```

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped Flawfinder version, but also base image / orb / dependency bumps. Follow it top to bottom; it tells you what to change, how to regenerate derived files, how to test locally, and how to interpret CI so you can iterate on failures without guessing.

### 1. What this repository is

This is a **Codacy engine**: a thin Scala wrapper (`src/main/scala/codacy/Engine.scala`, built on `codacy-engine-scala-seed`) that packages [Flawfinder](https://dwheeler.com/flawfinder/) — a Python-installed C/C++ security-weakness scanner — as a Docker image Codacy's platform can run against a customer's source code. The `src/main/resources/docs/` directory is not just documentation — it is **machine-consumed configuration**:

- `docs/patterns.json` — the full list of Flawfinder rules ("patterns") Codacy knows about, their security category/subcategory, severity level, and which are enabled by default. Generated file, do not hand-edit.
- `docs/description/description.json` — human-readable title/description per pattern, used in the Codacy UI. Generated file, do not hand-edit.
- `docs/tests/*` and `docs/multiple-tests/*` — fixtures used by `codacy-plugins-test` to validate the engine actually produces the results it claims to for real code samples.
- `docs/tool-description.md` — short blurb about the tool, hand-maintained.

Both generated artifacts above come from **`DocGenerator`** (`src/main/scala/codacy/flawfinder/DocGenerator.scala`), driven by the `generate-docs` target in the `Makefile`. That target runs the freshly built `codacy-flawfinder-base` Docker image with `flawfinder -QD --listrules` to dump the tool's own rule list, then feeds that output into `sbt "runMain codacy.flawfinder.DocGenerator .tmp_errorlist"`. This means regeneration needs **Docker** and **sbt** locally, but no network access to a third-party doc site — the rule list comes straight from the installed `flawfinder` binary. Which patterns are enabled by default is a separate hand-maintained list in `src/main/scala/codacy/flawfinder/DefaultPatterns.scala`; new rule IDs introduced by a version bump will not be enabled unless added there.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `.flawfinder-version` | The exact Flawfinder release downloaded and built into the Docker base image (`Dockerfile` fetches `https://dwheeler.com/flawfinder/flawfinder-$toolVersion.tar.gz` using this value via `make publish-base`) | Bump to the target version; confirm that tarball actually exists at that URL on dwheeler.com. |
| `build.sbt` → `com.codacy" %% "codacy-engine-scala-seed"` | Shared Codacy engine framework version | Bump if a newer seed is published. |
| `project/plugins.sbt` → `codacy-sbt-plugin`, `sbt-native-packager` | sbt build plugins | Bump if newer versions are published. |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI checkout/build/version steps | Check the latest published version. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI | Same as above. |
| `Dockerfile` → `FROM amazoncorretto:...` base image | Runtime the packaged app runs on | Only bump if the new tool version raises its minimum runtime requirement (e.g. a newer Python). |

### 3. Step-by-step update procedure

1. **Bump `.flawfinder-version`** (or other version(s)) as scoped by the task.
2. **Rebuild the base image and regenerate the docs**: `make all` (runs `publish-base` then `generate-docs`, per the `Makefile`). Review the diff to `src/main/resources/docs/patterns.json` and `description.json` for new/removed/renamed rule IDs, and check `DefaultPatterns.scala` for whether any new rule should be added to the enabled-by-default list. Also check `docs/multiple-tests/*/results.xml` fixtures for rule IDs that no longer appear or whose severity changed.
3. **Compile and format**: `sbt "set scalafmtUseIvy in ThisBuild := false; scalafmt::test; test:scalafmt::test; sbt:scalafmt::test"` (mirrors the CI step) — fix any formatting failures with `sbt scalafmt`.
4. **Build the Docker image locally**: `sbt docker:publishLocal` (or just `make all && sbt docker:publishLocal` per the README's own Usage section).
5. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run the relevant DockerTest commands against your local image tag, exercising the fixtures under `docs/tests/` and `docs/multiple-tests/`.
6. **Iterate on failures**, re-running only the relevant test command after each fix.
7. **Commit** the version bump(s) together with any regenerated `patterns.json`/`description.json` files in one change.
8. **Push and open a PR.**
9. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `wget` in `Dockerfile`/`make publish-base` fails to fetch the tarball | `.flawfinder-version` points at a version that was never published at `https://dwheeler.com/flawfinder/flawfinder-<version>.tar.gz` | Verify the exact release tag/filename on dwheeler.com before bumping. |
| `docker:publishLocal` fails with a base-image error | `dockerBaseImage := "codacy-flawfinder-base"` in `build.sbt` refers to the locally built base image | Run `make publish-base` (or `make all`) first so that image exists before running sbt's docker task. |
| New rules missing from Codacy's UI / not flagged even though Flawfinder reports them | New rule IDs introduced upstream aren't present in `DefaultPatterns.scala`'s enabled-by-default list | After regenerating `patterns.json`, diff the rule ID set against the previous version and add newly-relevant IDs to `DefaultPatterns.list` if they should be on by default. |

### 5. Definition of done

- Version bump(s) reflected in all files that encode them (§2).
- `patterns.json` and `description.json` regenerated via `make all` and committed, with `DefaultPatterns.scala` and test fixtures reconciled against any rule-set changes.
- `scalafmt` checks pass locally.
- Docker image builds successfully (`make publish-base` + `sbt docker:publishLocal`).
- `codacy-plugins-test` commands all pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
