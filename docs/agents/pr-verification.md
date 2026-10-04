# Agentic PR verification

> Moved verbatim out of `AGENTS.md` on 2026-10-04 so that file fits the 32 KiB Codex reads
> by default. Its rules still bind: `AGENTS.md` lists the hard ones inline and says when to
> read this file. Edit the rule here, not a copy of it.

## Agentic PR verification (MANDATORY on every PR)

**Every PR MUST be verified end-to-end before merge, and the verdict MUST be posted as a PR
comment** via `gh pr comment`. A headless agent (`claude -p`, local) drives the running app and
posts the result; it **never merges** — it waits for you. Running the pass and posting the verdict
comment is **not optional**. It catches what the diff and unit tests miss: missing controls,
unimplemented protocols/screens, dead flows, a UI that doesn't match the spec.

- **Engine.** Native (Android) → **mobile-mcp** — the mobile counterpart to Playwright MCP: it navigates the native **accessibility tree** over `adb` and only falls back to screenshot coordinates when labels are missing. Run it against an **emulator or a dedicated test device, never your daily phone**. Alternative with more stable locators: appium-mcp (UiAutomator2).
- **Reliability key = semantics.** `Modifier.testTag(...)`, `contentDescription`, `Modifier.semantics { }` (or accessibility labels on classic Views). Without them the agent falls back to fragile coordinates. Audit that the flows you verify (protocol selection, start/stop playback) are labeled first.
- **Two layers.** Deterministic suites (spec-derived encoder unit tests, Espresso/Compose UI tests) stay the **hard merge gate**; the agentic pass is **advisory on the merge decision** — it explores the new surface, writes the missing regression tests, and leaves a readable verdict, and it **never vetoes a merge on its own**. But running it and posting the verdict comment is **mandatory**, not advisory. (It cannot judge real RF lock — that stays the hardware/real-signal smoke pass.)
- **The verdict reads structure too.** Besides driving the app, it names what the diff does to the [Design principles](design-principles.md#design-principles--solid-applied-with-judgement): a new violation (UI importing `AudioTrack` directly, one more `AntennaType` branch added to a growing `when`) or a new speculative abstraction. Findings, not a veto — like the rest of the pass.
- **Hard limits.** The verdict awaits your close and the agent **never merges** (see *Git & GitHub*). Point it at a dedicated emulator/test device; scope `--allowedTools`; use `--dangerously-skip-permissions` only in a controlled local env.
