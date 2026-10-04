# Quality beyond coverage

> Moved verbatim out of `AGENTS.md` on 2026-10-04 so that file fits the 32 KiB Codex reads
> by default. Its rules still bind: `AGENTS.md` lists the hard ones inline and says when to
> read this file. Edit the rule here, not a copy of it.

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
