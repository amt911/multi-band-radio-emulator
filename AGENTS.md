# Multi Band Radio Emulator — Agent Guide

Android app that emulates longwave **time-signal** radio broadcasts (DCF77, MSF, WWVB, JJY40/JJY60, BPC). It synthesizes the amplitude-modulated carrier of each national time station second-by-second and plays it through the device speaker via `AudioTrack`, so a nearby radio-controlled clock can pick it up and synchronize. UI is Jetpack Compose (Material 3).

## Start here

Run `/graphify` before each session. The persistent graph at `graphify-out/graph.json` summarizes architecture, dependencies, and cross-cutting concepts without re-reading the repo each time.

## Agent compatibility — Codex and Claude Code

This file is `AGENTS.md`: the **one** instruction file for every coding agent in this repo. Codex reads it directly; Claude Code reads `CLAUDE.md`, which only imports this file (`@AGENTS.md`) and holds what applies to Claude alone. **Edit rules here, never in `CLAUDE.md`** — two copies of a rule drift apart on the first edit, and each agent then obeys a different one.

| Concern | Claude Code | Codex |
| --- | --- | --- |
| Instruction file | `CLAUDE.md` → imports `AGENTS.md` | `AGENTS.md` (root down to the working directory) |
| Invoke a skill | `Skill` tool, or `/<skill>` | mention it (`$<skill>`), or let it trigger from its description |
| Skills on disk | `~/.claude/skills` (links into `~/.agents/skills`) | `.agents/skills`, then `~/.agents/skills` |
| superpowers | `superpowers@claude-plugins-official` (`/plugin install`) | `superpowers@openai-curated` (install from `/plugins`; that id is its key in `~/.codex/config.toml`) |
| MCP servers | `claude mcp add -s user <name> -- <cmd>` | `codex mcp add <name> -- <cmd>` (`~/.codex/config.toml`) |
| File size | imports load whole | `project_doc_max_bytes`, **32 KiB by default** — raise it when this file is bigger, or the tail is silently dropped |

- **Install shared skills once, for both agents:** `npx skills add <owner/repo> -g --skill <name>` writes to `~/.agents/skills` and links it for Claude Code, so both run the same version.
- **Names in this file are capabilities, not one agent's syntax.** "Invoke the `X` skill" means the `Skill` tool in Claude Code and a skill mention in Codex. An MCP server named here is used when it is registered for the agent you are running in; its absence never blocks ordinary work.
- **Modes, model caps and Git rules bind both agents.** "lite mode", "normal mode" and "modo desatendido" mean the same in Codex; a cap written as "no model above Sonnet" means "no model above the mid tier" there.
- **Claude-only commands** (`/graphify` and other slash commands that are not skills) are skipped by Codex unless the same capability is installed as a skill in `~/.agents/skills`.

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

## UI/UX workflow — stack-aware

**Wide latitude in how the UI is made, no latitude in whether it came out well.** The agent may reach for any tool, library or technique below — or none of them — and may push the design well past Material 3's default look. What it may not do is call UI done before the **real rendered surface has been observed, compared with the design context, critiqued, corrected and exercised end-to-end**. A single prompt-to-code pass is not a design loop.

### Sources of truth

1. **Root `PRODUCT.md` + `DESIGN.md` belong to Impeccable.** Neither exists in this repo yet — run `$impeccable teach` before the first deliberate UI change; do not hand-author them or let another tool overwrite them. They define product personality, audience and visual direction.
2. **`design-system.md` is the portable design contract** — semantic color/type/shape, components, states, motion, accessibility. It doesn't exist yet either; Compose maps it to `MaterialTheme`/Material 3 once it does.
3. **Generated design documents never land on the root files.** If a tool like Stitch's `extract-design-md` is ever used, save its output below `docs/design/` with an explicit name and bring over only the decisions you keep.

### Creative latitude — the ceiling is the product, not the component library

- **Material 3 is a floor, not a ceiling.** Bespoke and expressive components, choreographed motion, custom drawing and shaders are welcome wherever they serve the direction in `PRODUCT.md` / `DESIGN.md` — a signal visualizer is exactly the kind of screen that can earn a bespoke `Canvas` treatment. Impeccable's `bolder`, `delight`, `animate`, `colorize`, `typeset` and `overdrive` push a design further; `quieter` and `distill` pull it back.
- **Three conditions, no exceptions:** the work derives from `design-system.md` tokens once that file exists (a value it lacks is **added to the system first**, then used — no magic numbers); motion honours the platform's reduced-motion setting and is never required to understand or finish a task; and the result passes [UI done means observed](#ui-done-means-observed-not-generated).
- **Where bespoke code lives.** Build on Material 3 slots and theming first (`ui/theme/`); `Canvas`, `graphicsLayer` and AGSL shaders are fair game for signature moments like `SignalVisualizerCard`.
- **Generic is a defect.** An untouched neutral theme, stock gradients, emoji as icons — Impeccable's anti-pattern catalogue is the reference a critique measures against.

### Explore wide, then converge

For a **new screen, a redesign or a signature moment** (e.g. the signal visualizer), render **two or three genuinely different directions** (layout, type scale, density, motion) in the real stack before settling. Compare them against `PRODUCT.md` / `DESIGN.md`, pick one, and write in the spec or PR why it won — the losers are deleted, not kept as dead variants. Small changes to an existing screen skip this step.

### Toolbox — capabilities, not dependencies

The agent chooses. Each row names a **default** and **when to reach for something else**; none is a project dependency, and a missing one never blocks work — explain what it would add and ask before installing it. MCP names are the ones used by `claude mcp add` / `codex mcp add`; skills install for both agents with `npx skills add <owner/repo> -g --skill <name>` (see [Agent compatibility](#agent-compatibility--codex-and-claude-code)).

**Privacy boundary:** never send source, screenshots, user data or a running private UI to a **hosted** service (Stitch, 21st, Figma's remote server, Gemini) without explicit approval. Inspecting a local app with a local MCP server is not permission to upload it.

**Coexistence:** one generator per component — never splice the output of two generators into one piece.

#### Android / Jetpack Compose

| Role | Default | Reach for instead when… | Runs |
| --- | --- | --- | --- |
| Direction and taste | Impeccable + `android/skills` `adaptive`, `styles`, `edge-to-edge` | an unofficial Material 3 Expressive skill, as reference only | local |
| Components | Material 3 composables, slots and theming | custom `Canvas` / `graphicsLayer` / AGSL for signature moments (e.g. the waveform visualizer) | local |
| Compose idiom | `chrisbanes/skills`: `compose-component-design`, `compose-state-and-effects`, `compose-animations` | `compose-performance` on jank; `compose-focus-navigation` for keyboard and accessibility focus | local |
| Observe a composable | `android studio render-compose-preview --print-semantics --output-image-file=<png> <file.kt> <PreviewFn>` — PNG plus semantics JSON; needs Android Studio Quail 2 Canary 1 or later running, with Gemini enabled and signed in | Compose Preview Screenshot Testing, headless: `./gradlew updateDebugScreenshotTest` / `validateDebugScreenshotTest` (HomeScreen, AntennaInfoScreen, OptionsScreen previews under `app/src/screenshotTest/`) | local |
| Observe the running app | `android screen capture --output=<png>` + `android layout --pretty` on an emulator or device | `maestro hierarchy` | local |
| Performance | `android-profiler` skill + `compose-performance` | — | local |
| AI bootstrap | — | Gemini "Transform UI" / image-to-Compose in Android Studio: manual, IDE-only, a first draft at best | hosted |
| Deterministic gate | `.maestro/` (`maestro test .maestro/`) — one smoke flow committed so far, more owed as screens grow behaviour | the mandatory [Agentic PR verification](#agentic-pr-verification-mandatory-on-every-pr) pass below stays the advisory layer on top | local |

- **`android` CLI — load the `android-cli` skill before using it.** The skill carries the verified
  commands: `android run` (build, install, launch), `android emulator list|start|stop`,
  `android screen capture --output=<png>`, `android layout --pretty`, `android docs search "<keywords>"`
  (official docs, instead of guessing an API), `android describe` (build targets and APK paths).
  More official skills: `android skills list` · `android skills add <id>`.

### The loop

```text
PRODUCT.md + DESIGN.md + design-system.md (once they exist) + the spec at hand
                        ↓
   explore wide (2–3 real directions) → converge, reasons written down
                        ↓
     build: Material 3 primitives first, bespoke Canvas/shaders where the product needs it
                        ↓
          observe the REAL render / preview (pixels + semantics)
                        ↓
  critique (Impeccable) → correct → observe again   ← repeat until it holds
                        ↓
            polish → performance measured → a11y audited
                        ↓
   deterministic E2E (`.maestro/`, growing) → the Agentic PR verification pass
```

**Never accept the first render.** Inspect the primary screen plus its loading, empty, error, disabled and validation states; every window size class; both themes; focus, keyboard and touch behaviour; contrast; text overflow and long translations (this app ships `values` and `values-es`); and the accessibility/semantics tree. A UI that matches a screenshot but breaks in dark mode or under TalkBack is not polished.

### Native Android / Jetpack Compose

Compose must feel like Android, not like CSS translated to Kotlin:

- **Map the design contract to Material 3** — `ColorScheme`, `Typography`, `Shapes` in `ui/theme/`, plus dimension and domain tokens once `design-system.md` exists. Prefer Material 3 components, slots and adaptive patterns; go custom (as `SignalVisualizerCard` already does) where the product's signature asks for it.
- **See the pixels before reasoning about them.** Render the composable (`render-compose-preview` or screenshot tests) at the configurations that matter — font scale, dark theme, RTL — and read the semantics JSON, not only the image.
- **`.maestro/` has one smoke flow so far** (`01-launch.yaml`, tag `smoke`) — it proves the app boots and the scaffold renders, nothing more. Previews and screenshots catch visual problems; real per-screen navigation and behaviour coverage (more Maestro flows, or a Compose UI test) is still open work as new screens ship.

### UI done means observed, not generated

Before calling UI work complete, verify all of these that apply:

- the real render/preview was inspected **after the final code change**, not only before it;
- for a new screen, redesign or signature moment, directions were explored and the choice is written down;
- both supported themes and the relevant window size classes were checked;
- loading/empty/error/disabled/validation states were seen, not inferred from source;
- touch targets plus accessibility semantics (`contentDescription`, `Modifier.semantics { }`) are usable, and reduced motion is honoured;
- performance of the main interaction was measured (no dropped frames in the profiler), not assumed;
- every new value exists as a token in `design-system.md` once that file exists;
- the result was compared against `PRODUCT.md` / `DESIGN.md` (once written), then critiqued and polished;
- **the mandatory Agentic PR verification pass ran and posted its verdict** — it is this repo's only end-to-end gate today; there is no deterministic Maestro/Espresso suite to fall back on.

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
- **The verdict reads structure too.** Besides driving the app, it names what the diff does to the [Design principles](#design-principles--solid-applied-with-judgement): a new violation (UI importing `AudioTrack` directly, one more `AntennaType` branch added to a growing `when`) or a new speculative abstraction. Findings, not a veto — like the rest of the pass.
- **Hard limits.** The verdict awaits your close and the agent **never merges** (see *Git & GitHub*). Point it at a dedicated emulator/test device; scope `--allowedTools`; use `--dangerously-skip-permissions` only in a controlled local env.

## Design principles — SOLID, applied with judgement

SOLID is a list of **symptoms to look for**, not a pattern to apply. Every one of the five exists to keep a change local: the useful question is *how many files does the next plausible change touch, and how many of them do you have to understand first?* Applied by rote it produces the opposite — an interface per class, a factory for one product, an eight-file feature — so here it is bounded by YAGNI and by reuse-first thinking: extend or parameterize what exists before adding a new seam.

| Principle | Checkable smell | Usual fix |
| --- | --- | --- |
| **S — Single responsibility**: one reason to change | the description needs "and"; a `*Renderer` both computes PCM samples and touches `AudioTrack`; a screen both fetches state and lays out | split along the reason to change — encoding, playback, presentation |
| **O — Open/closed**: extend without editing | adding a protocol edits a growing `when (antennaType)` in several files instead of one dispatch point; one boolean prop per variant | a variants map, strategy, slot or registry — introduced at the second real case, not the first |
| **L — Liskov substitution**: subtypes keep the contract | a `*Renderer` throws or returns nonsense for a bit value the base contract promises to handle; callers check the concrete protocol before calling | narrow the base contract, or stop inheriting and compose |
| **I — Interface segregation**: clients see only what they use | a fake `TimeSignalRenderer` implements methods a test never calls; a whole `AntennaType` is passed to read one field | split by client need; pass the fields, not the bag |
| **D — Dependency inversion**: policy does not import mechanism | `audio/*/Record.kt` or `Renderer.kt` imports `android.media.AudioTrack` or `android.util.Log` directly, instead of staying pure Kotlin; a unit test needs a device to run | depend on a port the caller owns (interface, function); wire `AudioTrack`/`Log` at the `RadioSignalPlayer` boundary |

### In UI code

- **S:** a Compose screen **presents or orchestrates**, not both. The state holder computes values; the composable renders them — which is also what lets a preview or screenshot test render it with fake state.
- **O:** a new look is a **new variant or composable parameter with a default**, not another `if`/`when` branch inside an existing composable.
- **L:** every variant keeps the base's guarantees — disabled state, focus, semantics/`contentDescription`, touch target. An expressive component that drops accessibility is not a variant; it is a regression with a nicer look.
- **I:** narrow parameters; slot lambdas over configuration objects; never a whole `AntennaType`/domain object to render its name.
- **D:** UI depends on a state holder/ViewModel, never on `AudioTrack` or `AudioRecord` directly.

### Where the seams go, per stack

| Stack | Seams |
| --- | --- |
| Android (Compose) | stateless composables + a state holder; `TimeSignalRenderer`/`TimeSignalRecord` interfaces at the encoding boundary; `AudioTrack`/`Log` confined to `RadioSignalPlayer` at the playback edge |

### Where SOLID stops

- **No interface, abstract class or factory without one of:** a second real implementation, an IO boundary (the `AudioTrack` playback edge, the system clock), or a test that cannot be written without the seam. "We might swap it later" is not on the list.
- **Reuse first beats speculative extension points:** add the parameter to the existing thing before inventing a plugin system for it.
- **Speculative abstraction is a review finding**, exactly like a violation: an interface with one implementation and no IO behind it gets inlined.
- **Refactor toward SOLID when a change hurts**, in the PR that felt the pain — not as a drive-by rewrite of code nobody is changing.

## Working rules

- **Use superpowers skills whenever they apply** — invoke via `Skill` before acting; process skills before implementation skills.
- **Don't add dependencies without asking** — the stack is intentional and minimal (no external audio/DSP libs on purpose). New libs go through `gradle/libs.versions.toml`.
- **TDD by default** for new/changed `audio/` logic. Don't merge encoding or rendering changes without host tests.
- **The spec rules the encoders** — any change to a `*Renderer`/`*Record` must stay consistent with `docs/TIME_SIGNAL_SPECIFICATIONS.md`; update the doc in the same change if the protocol understanding changes.
- **Keep protocol logic Android-free** — synthesis/encoding must stay pure Kotlin (host-testable); confine `AudioTrack`, `Log`, and other `android.*` calls to the playback boundary.
- **UI work → design context, then wide latitude, then the observed-quality gate** — invoke `impeccable` + applicable superpowers, let `$impeccable teach` write root `PRODUCT.md` / `DESIGN.md` if they don't exist yet (auto-migrating a legacy `.impeccable.md` → `PRODUCT.md`), then follow [UI/UX workflow — stack-aware](#uiux-workflow--stack-aware). Any tool, library or expressive technique is fair game if it derives from the design tokens; nothing is done until the real render was observed, critiqued, polished, and the mandatory Agentic PR verification pass is green — this repo has no deterministic Maestro/Espresso suite yet.
- **SOLID where it pays, not by rote** — split by reason to change, extend through variants or strategies, keep subtypes honest, keep interfaces and parameters narrow, and push IO (`AudioTrack`, `Log`, the system clock) behind a seam at the `RadioSignalPlayer` boundary. No abstraction without a second implementation, an IO boundary or a test seam. See [Design principles](#design-principles--solid-applied-with-judgement).

## Git & GitHub

- **Commits and branches OK** — create commits and new branches whenever it makes sense, without asking first.
- **Never push** *(default)* — no `git push` under any circumstance, and absolutely never `git push --force` / `--force-with-lease`. Leave pushing to the user. **Exception:** when **"modo desatendido"** is active, you may push the feature branches you create (never `main`/protected branches, never force) so PRs are ready for review.
- **Never merge — no permission** — you do NOT have permission to merge anything into any branch, nor to merge any pull request. No `git merge`, no fast-forward integration, no `gh pr merge`. Leave every merge (branches and PRs alike) to the user. This holds in every mode, **including "modo desatendido"**.
- **GitHub via `gh`** — if the `gh` CLI is available, you may open pull requests, issues, and similar (comments, labels, etc.). These don't require pushing on your part beyond what `gh` itself does for an already-pushed branch.
- **Every PR must include a manual test plan** — when opening a PR, add a **How to test manually** section describing the exact steps to exercise the change by hand. For this app: which screen to open, which `AntennaType` to select and play, and the expected result (playback starts/stops cleanly; a radio-controlled clock or SDR decodes the correct time). Include any setup (device vs emulator, connected clock/SDR) and edge cases (minute rollover, DST transition, switching protocols mid-playback).
