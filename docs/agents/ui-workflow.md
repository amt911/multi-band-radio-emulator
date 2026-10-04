# UI/UX workflow — stack-aware

> Moved verbatim out of `AGENTS.md` on 2026-10-04 so that file fits the 32 KiB Codex reads
> by default. Its rules still bind: `AGENTS.md` lists the hard ones inline and says when to
> read this file. Edit the rule here, not a copy of it.

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

The agent chooses. Each row names a **default** and **when to reach for something else**; none is a project dependency, and a missing one never blocks work — explain what it would add and ask before installing it. MCP names are the ones used by `claude mcp add` / `codex mcp add`; skills install for both agents with `npx skills add <owner/repo> -g --skill <name>` (see [Agent compatibility](agent-compatibility.md#agent-compatibility--codex-and-claude-code)).

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
| Deterministic gate | `.maestro/` (`maestro test .maestro/`) — one smoke flow committed so far, more owed as screens grow behaviour | the mandatory [Agentic PR verification](pr-verification.md#agentic-pr-verification-mandatory-on-every-pr) pass below stays the advisory layer on top | local |

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
