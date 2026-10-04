# Multi Band Radio Emulator — Agent Guide

Android app that emulates longwave **time-signal** radio broadcasts (DCF77, MSF, WWVB, JJY40/JJY60, BPC). It synthesizes the amplitude-modulated carrier of each national time station second-by-second and plays it through the device speaker via `AudioTrack`, so a nearby radio-controlled clock can pick it up and synchronize. UI is Jetpack Compose (Material 3).

## Start here

Run `/graphify` before each session. The persistent graph at `graphify-out/graph.json` summarizes architecture, dependencies, and cross-cutting concepts without re-reading the repo each time.

## ⚡ graphify — use every session

```
/graphify            # first run (builds graph from scratch)
/graphify --update   # incremental update (only re-extracts changed files)
/graphify query "<question>"    # architecture questions instead of opening multiple files
/graphify explain "<name>"      # locate a concept or symbol
/graphify path "A" "B"          # dependency path between two modules
```

Outputs in `graphify-out/`: `graph.json` (source of truth), `GRAPH_REPORT.md` (god nodes, communities, surprising connections), `graph.html` (interactive view).

Run `/graphify --update` at end of session if you touched docs or images (code changes rebuild via hook if installed).

## ⚡ superpowers — use whenever applicable

Always prefer **superpowers** skills over ad-hoc approaches. If there's even a small chance a skill applies to the task, invoke it via the `Skill` tool before acting (including before clarifying questions).

- **Process skills first** — `brainstorming` before creative/feature work, `systematic-debugging` before fixing bugs, `test-driven-development` before writing implementation.
- **Then implementation skills** — domain-specific skills guide execution.
- **Verify before claiming done** — `verification-before-completion` / `requesting-code-review` before merging.

User instructions always take precedence over skills; skills override default behavior.

### Mode switch

- **"lite mode"** — fully disables superpowers: no skill is invoked, not even the applicability check, until **"normal mode"** is said.
- **"normal mode"** (default) — standard superpowers behavior, plus: when delegating coding work, dispatch at most 1 agent at a time, and never use a model above Sonnet (no Opus).
- **"modo desatendido"** (unattended mode) — the user is away and delegates autonomy: work without waiting for confirmations and make reasonable decisions yourself instead of asking. In this mode you MAY **`git push` the feature branches you create** and **open PRs via `gh`** on your own, so the work is ready for review when the user returns. The hard limits still hold and are NOT lifted: **never merge anything** (no `git merge`, no fast-forward integration, no `gh pr merge`), **never push to `main`** or any protected/default branch directly, and **never** `git push --force` / `--force-with-lease`. Deliver everything as pushed branches + PRs for the user to merge. Reverts to defaults on **"normal mode"**.
  **Pace in this mode** (2026-10-04): intermediate tasks run only the tests of what they touched
  (`./gradlew testDebugUnitTest --tests '<package>.*'`) plus that task's Maestro flow; commits pile up
  locally and the branch is pushed **once, at the end**, after the final full pass (host tests,
  screenshot validation, Maestro). Each intermediate push paid the whole suite (minutes) to report
  nothing the next one would not.

Confirm the switch briefly when it happens.

## Rules by topic — what always binds, and where the detail lives

This file fits in the 32 KiB Codex reads by default (`wc -c AGENTS.md` ≤ 32768; when it grows, move
detail to `docs/agents/`, never raise the limit). The detail of each topic was moved verbatim to
`docs/agents/` on 2026-10-04. **The lines below bind even if you never open the document; open it
before working on that topic.** A rule is edited in its document, not here and there at once —
except for its one-line summary in this list.

- **UI/UX workflow** → [docs/agents/ui-workflow.md](docs/agents/ui-workflow.md), before touching a
  screen or component. `impeccable` first; `PRODUCT.md` / `DESIGN.md` never by hand; nothing is done
  until the real render was observed after the last change; never send source or screenshots to a
  hosted service without explicit approval.
- **Quality beyond coverage** →
  [docs/agents/quality-beyond-coverage.md](docs/agents/quality-beyond-coverage.md). The spec
  (`docs/TIME_SIGNAL_SPECIFICATIONS.md`) is the oracle, never the code's current output; property
  tests and Pitest on `audio/`; a real-signal smoke; the AI never defines the acceptance criteria.
- **Agentic PR verification (mandatory)** → [docs/agents/pr-verification.md](docs/agents/pr-verification.md).
  Every PR gets the verdict of a pass that drives the running app as a PR comment; it never merges.
- **Design principles (SOLID)** → [docs/agents/design-principles.md](docs/agents/design-principles.md).
  No abstraction without a second implementation, an IO boundary or a test seam.
- **Codex and Claude Code** → [docs/agents/agent-compatibility.md](docs/agents/agent-compatibility.md).
  Rules are edited in `AGENTS.md` (or its `docs/agents/` document), never in `CLAUDE.md`.
- **Remote Gradle cache (Reposilite)** → [docs/BUILD-CACHE.md](docs/BUILD-CACHE.md). If
  `~/.gradle/gradle.properties` lacks `gradleCacheUrl`, tell the user this machine isn't wired yet
  (section *Las máquinas de desarrollo leen*); never set `gradleCachePush=true` outside CI.

## Stack

- **Android** — `minSdk 24`, `targetSdk 36`, `compileSdk 36`. Application id `com.example.multibandradioemulator`.
- **Kotlin** 2.0.21 (JVM target 11) — all source is Kotlin, no Java/C/C++/NDK.
- **Jetpack Compose** (BOM 2024.09.00) + **Material 3** — entire UI. Single-Activity (`MainActivity`), no XML layouts.
- **Navigation Compose** 2.8.4 — bottom-nav between Home / Options / Antenna Info screens (`navigation/BottomNavItem.kt`).
- **`android.media.AudioTrack`** — real-time 48 kHz, 16-bit mono PCM streaming. No external audio/DSP libraries; waveform synthesis is hand-written with `kotlin.math`.
- **Gradle** (Kotlin DSL) with a version catalog at `gradle/libs.versions.toml`; AGP 9.0.1. Use the wrapper `./gradlew`.

### Layout

- `app/src/main/java/.../audio/` — the domain core. `TimeSignalRenderer` (interface) + `TimeSignalRecord` + `SignalShape`; `RadioSignalPlayer` drives playback on a background thread synced to the system clock. One sub-package per protocol: `dcf77/`, `msf/`, `wwvb/`, `jjy/`, `bpc/`, each with a `*Renderer` (PCM generation) and `*Record` (encoded time bits). `bpc/BpcBitString.kt` holds pure bit-encoding logic.
- `app/src/main/java/.../model/` — `AntennaType` enum (the six protocols).
- `app/src/main/java/.../ui/` — Compose `screens/`, reusable `components/` (e.g. `SignalVisualizerCard`), and `theme/`.
- `docs/TIME_SIGNAL_SPECIFICATIONS.md` — authoritative protocol reference (carrier freq, modulation scheme, bit layout per station). **Read this before touching any renderer/record** — the encoding must match the real spec or a real clock won't sync.
- Not on `main` yet, but real and in flight on the `fixes` branch: two root-level directories, `dcf77-soundwave/` and `timestation/`, recorded as gitlinks (no `.gitmodules`, so they're nested checkouts rather than a configured submodule). Treat them as **vendored third-party references** for the protocol specs once they land — don't document their internals here, and don't edit inside them.

## Commands

Use the Gradle wrapper (`./gradlew`) from the repo root. Building requires an Android SDK (`local.properties` / `ANDROID_HOME`); it is gitignored and not present in a fresh checkout. Builds are slow — avoid running them speculatively.

```bash
# build
./gradlew assembleDebug          # build the debug APK
./gradlew :app:compileDebugKotlin # faster: just compile, catch Kotlin errors

# host unit tests (JVM, no device/emulator)
./gradlew testDebugUnitTest

# instrumented tests (needs a connected device or running emulator)
./gradlew connectedDebugAndroidTest

# install & run on a device/emulator
./gradlew installDebug
adb shell am start -n com.example.multibandradioemulator/.MainActivity

# lint
./gradlew lint                   # Android Lint report in app/build/reports/lint-results-debug.html

# screenshot tests (Compose Preview Screenshot Testing — host-side, LayoutLib; @PreviewTest
# previews live under app/src/screenshotTest/)
./gradlew updateDebugScreenshotTest      # (re)generate reference PNGs after an intentional UI change
./gradlew validateDebugScreenshotTest    # compare against references — fails on visual drift

# E2E (Maestro — emulator only)
maestro test .maestro/                        # whole suite (a directory works)
maestro check-syntax .maestro/*.yaml          # exits 1 on an invalid command — a real gate
```

## Tests and quality

The test suite is currently only the Android Studio scaffold stubs (`ExampleUnitTest`, `ExampleInstrumentedTest`) — there is **no real coverage yet**. Treat building it out as part of any substantive change to the audio core.

- **JUnit4** (host / `app/src/test/`) — the right home for the deterministic domain logic: time-bit encoding in each `*Record` / `BpcBitString`, parity/BCD helpers, and the pure parts of each `*Renderer` (e.g. that a given `ZonedDateTime` yields the expected modulation pattern per second). These run on the JVM with no device and should be the bulk of the tests.
- **Compose UI test + Espresso** (instrumented / `app/src/androidTest/`) — for screen behavior and navigation; needs a device/emulator. Uses `androidx.compose.ui.test.junit4` and `androidx.test.espresso`.
- File/naming convention: host tests in `src/test/.../*Test.kt`, instrumented tests in `src/androidTest/.../*Test.kt`, mirroring the package of the code under test.

### What to test per area

| Area | What | Status |
| --- | --- | --- |
| `audio/*/…Record.kt`, `BpcBitString.kt` | Pure time→bits encoding: BCD, parity, next-vs-current-minute, DST/timezone edges. Deterministic, no Android deps | Pending |
| `audio/*/…Renderer.kt` | Per-second PCM shape: correct modulation duration/depth for each bit value; sample count = `sampleRate * 2` bytes | Pending |
| `audio/RadioSignalPlayer.kt` | Renderer selection per `AntennaType`, start/stop lifecycle, thread safety (`AtomicBoolean`). Isolate `AudioTrack` behind a seam to test host-side | Pending |
| `ui/screens`, `navigation` | Compose interactions and bottom-nav routing (instrumented) | Pending |

### TDD — required for new logic

For new/changed encoding and rendering logic in `audio/`:

1. **Red** — write a failing host test asserting the exact bits/PCM the spec requires (cite `docs/TIME_SIGNAL_SPECIFICATIONS.md`).
2. **Green** — implement the minimum to pass.
3. **Refactor** — clean up under green tests.

Exceptions (TDD not required): pure Compose visual/style/copy changes, and theme tweaks. Add tests before merging any protocol-behavior change.

Rules:
- **Test over mock**: exercise the real encoders; mock only the Android edge (`AudioTrack`, `Log`).
- Keep protocol logic **free of Android imports** so it stays host-testable — push `AudioTrack`/`Log` to the boundary.

### The pyramid per feature — one E2E per journey, the rest one layer down

**Rule since 2026-10-04**, ported from the Android client, where a Maestro flow cost minutes and
every tap seconds. A new feature gets **one Maestro flow per main journey** (`.maestro/`, tagged like
`01-launch.yaml`): launch, do the one thing the feature is for, see the result. Everything else —
every bit/BCD/parity case, every `*Renderer` PCM shape, `AntennaType` selection, a screen's states
and copy — goes one layer down: JUnit4 host tests of the pure `audio/` logic
(`./gradlew testDebugUnitTest`) or Compose previews/screenshot tests (`@PreviewTest`, host-side),
which cost seconds and need no emulator.

- **When one more E2E is right:** what no host test can answer — a cold start, real `AudioTrack`
  output starting and stopping, navigation across screens on a real device, permissions and system
  dialogs, behaviour across rotation or process death. The flow's header says why it is not a host test.
- **A bug still gets its failing test first**, at the lowest layer that reproduces it; a protocol
  bug is reproduced as a host test asserting the exact bits.
- **Existing tests are not migrated for this rule.** It applies to new work and to what a change touches.

### Running the suites — the whole suite once at the end, only the reds in between

- **While working:** only the tests of what you touched —
  `./gradlew testDebugUnitTest --tests '<package>.*'` (or one class); a Maestro flow only for the
  journey you changed: `maestro test .maestro/<flow>.yaml`. Builds are slow: no speculative runs.
- **The full run happens once, at the end of the branch, alone:** `./gradlew testDebugUnitTest`,
  `./gradlew validateDebugScreenshotTest`, then `maestro test .maestro/` on the emulator — in the
  background while you write the PR, under `timeout --kill-after=60s <limit>`. Push and PR only
  after it is green.
- **Red pass → only the reds** until they are green or proven red on the base commit too: Gradle by
  class (`--tests`), Maestro by flow file (it has no last-failed flag). Then **one** full
  confirmation pass, the one that catches a fix breaking another test.
- **Three reds in a row on one test → stop** and read the evidence (the Gradle test report, the
  Maestro screenshots and hierarchy, `adb logcat`) before a fourth change.
- **No fixed sleeps** — wait on the state (Maestro's `assertVisible` / `extendedWaitUntil`, not a
  sleep). **Every heavy command** (Gradle builds, screenshot validation, Maestro) runs under
  `timeout --kill-after=60s <limit>`.

## Working rules

- **Use superpowers skills whenever they apply** — invoke via `Skill` before acting; process skills before implementation skills.
- **Don't add dependencies without asking** — the stack is intentional and minimal (no external audio/DSP libs on purpose). New libs go through `gradle/libs.versions.toml`.
- **TDD by default** for new/changed `audio/` logic. Don't merge encoding or rendering changes without host tests.
- **The spec rules the encoders** — any change to a `*Renderer`/`*Record` must stay consistent with `docs/TIME_SIGNAL_SPECIFICATIONS.md`; update the doc in the same change if the protocol understanding changes.
- **Keep protocol logic Android-free** — synthesis/encoding must stay pure Kotlin (host-testable); confine `AudioTrack`, `Log`, and other `android.*` calls to the playback boundary.
- **UI work → design context, then wide latitude, then the observed-quality gate** — invoke `impeccable` + applicable superpowers, let `$impeccable teach` write root `PRODUCT.md` / `DESIGN.md` if they don't exist yet (auto-migrating a legacy `.impeccable.md` → `PRODUCT.md`), then follow [UI/UX workflow — stack-aware](docs/agents/ui-workflow.md#uiux-workflow--stack-aware). Any tool, library or expressive technique is fair game if it derives from the design tokens; nothing is done until the real render was observed, critiqued, polished, and the mandatory Agentic PR verification pass is green — this repo has no deterministic Maestro/Espresso suite yet.
- **SOLID where it pays, not by rote** — split by reason to change, extend through variants or strategies, keep subtypes honest, keep interfaces and parameters narrow, and push IO (`AudioTrack`, `Log`, the system clock) behind a seam at the `RadioSignalPlayer` boundary. No abstraction without a second implementation, an IO boundary or a test seam. See [Design principles](docs/agents/design-principles.md#design-principles--solid-applied-with-judgement).

## Git & GitHub

- **Commits and branches OK** — create commits and new branches whenever it makes sense, without asking first.
- **Never push** *(default)* — no `git push` under any circumstance, and absolutely never `git push --force` / `--force-with-lease`. Leave pushing to the user. **Exception:** when **"modo desatendido"** is active, you may push the feature branches you create (never `main`/protected branches, never force) so PRs are ready for review.
- **Never merge — no permission** — you do NOT have permission to merge anything into any branch, nor to merge any pull request. No `git merge`, no fast-forward integration, no `gh pr merge`. Leave every merge (branches and PRs alike) to the user. This holds in every mode, **including "modo desatendido"**.
- **GitHub via `gh`** — if the `gh` CLI is available, you may open pull requests, issues, and similar (comments, labels, etc.). These don't require pushing on your part beyond what `gh` itself does for an already-pushed branch.
- **Every PR must include a manual test plan** — when opening a PR, add a **How to test manually** section describing the exact steps to exercise the change by hand. For this app: which screen to open, which `AntennaType` to select and play, and the expected result (playback starts/stops cleanly; a radio-controlled clock or SDR decodes the correct time). Include any setup (device vs emulator, connected clock/SDR) and edge cases (minute rollover, DST transition, switching protocols mid-playback).
