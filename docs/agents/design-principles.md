# Design principles — SOLID, applied with judgement

> Moved verbatim out of `AGENTS.md` on 2026-10-04 so that file fits the 32 KiB Codex reads
> by default. Its rules still bind: `AGENTS.md` lists the hard ones inline and says when to
> read this file. Edit the rule here, not a copy of it.

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
