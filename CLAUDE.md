# Multi Band Radio Emulator — Claude Guide

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

Confirm the switch briefly when it happens.

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

## Quality beyond coverage

**Coverage measures how much code runs, not whether it's correct.** This is especially treacherous with AI: it tends to write the test *and* the code in one move, so if it misread the requirement, both encode the same mistake and the test passes happily. For this app the trap is real — a renderer can produce audio that "sounds right" and passes a shallow test yet encodes the wrong minute, wrong parity, or wrong modulation timing, so an actual radio clock never syncs. These gates attack that blind spot.

- **Spec is the oracle** *(highest priority)* — the acceptance criteria come from `docs/TIME_SIGNAL_SPECIFICATIONS.md` and the source standards, **not** from what the code currently emits. Write the key asserts (expected bit at each second, reduction duration per bit value) from the spec by hand, then make the code match. Don't let the implementation define the expected values.
- **Property-based testing** — **kotest-property** (Kotlin) or **jqwik** (JUnit). Define invariants over generated times: encoding a full minute always yields exactly 60 seconds of data; BCD round-trips; parity bits are self-consistent; PCM byte length is always `sampleRate * 2` per second; amplitude never clips outside `[-1, 1]` before quantization. Generated cases surface the DST / minute-rollover / leap boundaries hand-picked examples miss.
- **Mutation testing** — **Pitest** (JVM, works on the host `testDebugUnitTest` sources) on the `audio/` package once real tests exist. A surviving mutant (`>` → `>=`, dropped parity, flipped bit) means covered-but-not-verified encoding.
- **Hardware/real-signal-in-the-loop smoke** — the ultimate check no unit test gives you: install on a device (`installDebug`), play each protocol, and confirm a real radio-controlled clock (or an SDR / a second phone decoding the audio) actually locks and shows the correct time. At minimum, boot the app on an emulator and verify each `AntennaType` starts/stops playback without crashing.
- **Strict static analysis** — keep Kotlin warnings clean; run **Android Lint** (`./gradlew lint`) and consider **detekt**/**ktlint** for style and complexity on the audio core.
- **Dependency hygiene** — dependencies are pinned via `gradle/libs.versions.toml`. AI invents non-existent packages and pulls vulnerable versions; verify every new library and version exists before adding it, and keep versions in the catalog (never hardcode in a `build.gradle.kts`).

**Process rule (worth more than any tool): don't let the AI define the acceptance criteria.** You write or review the important test cases yourself — the per-second bit expectations and the timezone/DST/minute-rollover edges — and have the AI implement against them. Mutation testing is the automated backstop; the judgment about *what the signal should be* stays with the spec and with you.

Priority by immediate payoff: **spec-derived host unit tests on the encoders first**, then **property-based invariants**, then **one hardware/real-signal smoke pass** per protocol.

## Agentic PR verification (MANDATORY on every PR)

**Every PR MUST be verified end-to-end before merge, and the verdict MUST be posted as a PR
comment** via `gh pr comment`. A headless agent (`claude -p`, local) drives the running app and
posts the result; it **never merges** — it waits for you. Running the pass and posting the verdict
comment is **not optional**. It catches what the diff and unit tests miss: missing controls,
unimplemented protocols/screens, dead flows, a UI that doesn't match the spec.

- **Engine.** Native (Android) → **mobile-mcp** — the mobile counterpart to Playwright MCP: it navigates the native **accessibility tree** over `adb` and only falls back to screenshot coordinates when labels are missing. Run it against an **emulator or a dedicated test device, never your daily phone**. Alternative with more stable locators: appium-mcp (UiAutomator2).
- **Reliability key = semantics.** `Modifier.testTag(...)`, `contentDescription`, `Modifier.semantics { }` (or accessibility labels on classic Views). Without them the agent falls back to fragile coordinates. Audit that the flows you verify (protocol selection, start/stop playback) are labeled first.
- **Two layers.** Deterministic suites (spec-derived encoder unit tests, Espresso/Compose UI tests) stay the **hard merge gate**; the agentic pass is **advisory on the merge decision** — it explores the new surface, writes the missing regression tests, and leaves a readable verdict, and it **never vetoes a merge on its own**. But running it and posting the verdict comment is **mandatory**, not advisory. (It cannot judge real RF lock — that stays the hardware/real-signal smoke pass.)
- **Hard limits.** The verdict awaits your close and the agent **never merges** (see *Git & GitHub*). Point it at a dedicated emulator/test device; scope `--allowedTools`; use `--dangerously-skip-permissions` only in a controlled local env.

## Working rules

- **Use superpowers skills whenever they apply** — invoke via `Skill` before acting; process skills before implementation skills.
- **Don't add dependencies without asking** — the stack is intentional and minimal (no external audio/DSP libs on purpose). New libs go through `gradle/libs.versions.toml`.
- **TDD by default** for new/changed `audio/` logic. Don't merge encoding or rendering changes without host tests.
- **The spec rules the encoders** — any change to a `*Renderer`/`*Record` must stay consistent with `docs/TIME_SIGNAL_SPECIFICATIONS.md`; update the doc in the same change if the protocol understanding changes.
- **Keep protocol logic Android-free** — synthesis/encoding must stay pure Kotlin (host-testable); confine `AudioTrack`, `Log`, and other `android.*` calls to the playback boundary.
- **UI work → design context first, then `impeccable` + superpowers** — for any Compose/UI change, invoke the `impeccable` skill (and its sub-skills: `shape`, `polish`, `critique`, etc.). **If the project has no design context (`PRODUCT.md` / `DESIGN.md` at the repo root), run `$impeccable teach`** — it explores the codebase and interviews you about the project's direction (register, users, brand personality, visual direction), then writes `PRODUCT.md` + `DESIGN.md` (auto-migrating a legacy `.impeccable.md` → `PRODUCT.md`); never hand-author it. Don't hand-roll UI without impeccable + superpowers.

## Git & GitHub

- **Commits and branches OK** — create commits and new branches whenever it makes sense, without asking first.
- **Never push** *(default)* — no `git push` under any circumstance, and absolutely never `git push --force` / `--force-with-lease`. Leave pushing to the user. **Exception:** when **"modo desatendido"** is active, you may push the feature branches you create (never `main`/protected branches, never force) so PRs are ready for review.
- **Never merge — no permission** — you do NOT have permission to merge anything into any branch, nor to merge any pull request. No `git merge`, no fast-forward integration, no `gh pr merge`. Leave every merge (branches and PRs alike) to the user. This holds in every mode, **including "modo desatendido"**.
- **GitHub via `gh`** — if the `gh` CLI is available, you may open pull requests, issues, and similar (comments, labels, etc.). These don't require pushing on your part beyond what `gh` itself does for an already-pushed branch.
- **Every PR must include a manual test plan** — when opening a PR, add a **How to test manually** section describing the exact steps to exercise the change by hand. For this app: which screen to open, which `AntennaType` to select and play, and the expected result (playback starts/stops cleanly; a radio-controlled clock or SDR decodes the correct time). Include any setup (device vs emulator, connected clock/SDR) and edge cases (minute rollover, DST transition, switching protocols mid-playback).
