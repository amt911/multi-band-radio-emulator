# Time Signal Specifications

## Overview

This document describes the technical specifications of the longwave time signal protocols emulated by this application. Each protocol is broadcast by a national standards laboratory via a low-frequency radio transmitter. Radio-controlled clocks use these signals to automatically synchronize their time.

All protocols share a common structure: a 60-second transmission cycle where each second carries one or more bits of encoded time data via amplitude modulation (AM) of the carrier wave. The modulation pattern (duration of carrier power reduction) within each second determines the transmitted bit value.

---

## Table of Contents

1. [DCF77 — Germany](#1-dcf77--germany)
2. [MSF — United Kingdom](#2-msf--united-kingdom)
3. [WWVB — United States](#3-wwvb--united-states)
4. [JJY40 — Japan (40 kHz)](#4-jjy40--japan-40-khz)
5. [JJY60 — Japan (60 kHz)](#5-jjy60--japan-60-khz)
6. [BPC — China](#6-bpc--china)

---

## 1. DCF77 — Germany

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | Physikalisch-Technische Bundesanstalt (PTB) |
| **Location** | Mainflingen, Germany (50°01′N, 09°00′E) |
| **Carrier Frequency** | 77.5 kHz |
| **Transmitter Power** | 50 kW |
| **Timezone** | CET/CEST (Central European Time) |
| **Coverage** | Central Europe (~2000 km radius) |

### Modulation Scheme

DCF77 uses **reduced-carrier-first** AM modulation. At the start of each second, carrier power drops (amplitude reduction) for a specific duration, then returns to full power.

| Bit Value | Reduction Duration | Meaning |
|-----------|-------------------|---------|
| `0` | 100 ms | Binary zero |
| `1` | 200 ms | Binary one |
| Minute marker | No reduction | Second 59: no amplitude drop (marks end of minute) |

- **AM deviation**: ~85% (carrier drops to ~15% of full amplitude during reduction)
- **Timing reference**: The leading edge of each second's power drop marks the exact second boundary.

### Time Code Encoding

DCF77 encodes the **next minute's** time. The data transmitted during a minute becomes valid when second 0 of the following minute begins.

The time/date values use **BCD (Binary-Coded Decimal)** encoding with **LSB first** (least significant bit transmitted first within each field).

| Second(s) | Bits | Field | Description |
|-----------|------|-------|-------------|
| 0 | 1 | Minute marker | Always 0 (start of minute) |
| 1–14 | 14 | Civil warning | Weather/disaster warnings (typically 0) |
| 15 | 1 | Call bit | Abnormal transmitter operation |
| 16 | 1 | Summer time announcement | 1 = DST change within next hour |
| 17 | 1 | CEST | 1 = Central European Summer Time active |
| 18 | 1 | CET | 1 = Central European Time active |
| 19 | 1 | Leap second announcement | 1 = leap second at end of current hour |
| 20 | 1 | Start of time | Always 1 |
| 21–27 | 7 | Minutes | BCD (0–59), LSB first. Bits: units (4) + tens (3) |
| 28 | 1 | Minute parity | Even parity over bits 21–27 |
| 29–34 | 6 | Hours | BCD (0–23), LSB first. Bits: units (4) + tens (2) |
| 35 | 1 | Hour parity | Even parity over bits 29–34 |
| 36–41 | 6 | Day of month | BCD (1–31), LSB first. Bits: units (4) + tens (2) |
| 42–44 | 3 | Day of week | BCD (1=Monday to 7=Sunday), LSB first |
| 45–49 | 5 | Month | BCD (1–12), LSB first. Bits: units (4) + tens (1) |
| 50–57 | 8 | Year | BCD (0–99), LSB first. Bits: units (4) + tens (4) |
| 58 | 1 | Date parity | Even parity over bits 36–57 |
| 59 | — | Minute marker | No carrier reduction (distinguishes from bit 0) |

### Parity Rules

- **P1 (bit 28)**: Even parity over bits 21–27 (minutes)
- **P2 (bit 35)**: Even parity over bits 29–34 (hours)
- **P3 (bit 58)**: Even parity over bits 36–57 (date: day, weekday, month, year)

### Example

To encode 14:32 CET on Wednesday, March 4, 2026:
- Bit 20 = 1 (start marker)
- Bits 21–27 = minutes 32: BCD = 0110010 → LSB first: 0100110
- Bits 29–34 = hours 14: BCD = 010100 → LSB first: 001010
- Bits 36–41 = day 4: BCD = 000100 → LSB first: 001000
- Bits 42–44 = weekday 3 (Wednesday): BCD = 011 → LSB first: 110
- Bits 45–49 = month 3: BCD = 00011 → LSB first: 11000
- Bits 50–57 = year 26: BCD = 00100110 → LSB first: 01100100

---

## 2. MSF — United Kingdom

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | National Physical Laboratory (NPL) |
| **Location** | Anthorn, Cumbria, UK (54°55′N, 03°16′W) |
| **Carrier Frequency** | 60 kHz |
| **Transmitter Power** | 17 kW |
| **Timezone** | UTC / BST (British Summer Time = UTC+1) |
| **Coverage** | United Kingdom and northwestern Europe |

### Modulation Scheme

MSF uses **on-off keying** (OOK): the carrier is completely shut off during modulation periods, unlike DCF77/WWVB which only reduce the carrier amplitude. Every second has at least 100 ms of carrier-off.

MSF conceptually transmits **two bits per second** (bit A and bit B), yielding four possible modulation patterns:

| Bit A | Bit B | Carrier-Off Pattern |
|-------|-------|---------------------|
| 0 | 0 | 100 ms off |
| 1 | 0 | 200 ms off |
| 0 | 1 | 100 ms off, 100 ms on, 100 ms off (split) |
| 1 | 1 | 300 ms off |

In practice, B is always 0 for seconds 1–52 (data range), and A is always 1 for seconds 53–58 (secondary minute marker). This means:

- **Seconds 1–52**: Only patterns `00` (100 ms off) and `10` (200 ms off) are used.
- **Seconds 53–58**: Only patterns `10` (200 ms off, if B=0) and `11` (300 ms off, if B=1) are used.

The "split" pattern (`01`) never occurs under normal operation.

Special seconds:
- **Second 0** (minute marker): 500 ms off
- **Second 59**: 100 ms off (base modulation only, no data)

The secondary minute marker (seconds 53–58) has A bits = `011111` and is always present. Combined with the B-stream parity/BST data, this produces recognizable 200–300 ms off patterns that receivers use for frame synchronization.

- **Encodes**: The **next minute's** time (like DCF77)

### Time Code Encoding — Bit A (time/date data)

Bit A carries the time and date information in **BCD format, MSB first**.

| Second(s) | Bits | Field | Description |
|-----------|------|-------|-------------|
| 0 | — | Minute marker | 500 ms off |
| 1–8 | 8 | DUT1 positive | Unary: number of 1s = DUT1 × 10 if DUT1 > 0 |
| 9–16 | 8 | DUT1 negative | Unary: number of 1s = |DUT1| × 10 if DUT1 < 0 |
| 17–20 | 4 | Year tens | BCD tens digit (0–9), MSB first |
| 21–24 | 4 | Year units | BCD units digit (0–9), MSB first |
| 25 | 1 | Month tens | BCD tens digit (0 or 1) |
| 26–29 | 4 | Month units | BCD units digit (0–9), MSB first |
| 30–31 | 2 | Day of month tens | BCD tens digit (0–3), MSB first |
| 32–35 | 4 | Day of month units | BCD units digit (0–9), MSB first |
| 36–38 | 3 | Day of week | 0 = Sunday, 6 = Saturday, MSB first |
| 39–40 | 2 | Hour tens | BCD tens digit (0–2), MSB first |
| 41–44 | 4 | Hour units | BCD units digit (0–9), MSB first |
| 45–47 | 3 | Minute tens | BCD tens digit (0–5), MSB first |
| 48–51 | 4 | Minute units | BCD units digit (0–9), MSB first |
| 52 | 1 | Unused | Always 0 |
| 53–58 | 6 | Secondary marker | Always `011111` (A = 1 for seconds 54–58, A = 0 for second 53) |

### Time Code Encoding — Bit B (status and parity)

Bit B is 0 for all seconds except 53–58, where it carries parity and BST information.

| Second | Field | Description |
|--------|-------|-------------|
| 1–52 | Reserved | Always 0 |
| 53 | BST warning | 1 = BST/GMT changeover within 61 minutes |
| 54 | Parity 1 | **Odd** parity over seconds 17A–24A (year) |
| 55 | Parity 2 | **Odd** parity over seconds 25A–35A (month + day of month) |
| 56 | Parity 3 | **Odd** parity over seconds 36A–38A (day of week) |
| 57 | Parity 4 | **Odd** parity over seconds 39A–51A (hour + minute) |
| 58 | BST flag | 1 = BST currently active for the transmitted time |
| 59 | — | No data (100 ms base carrier-off only) |

### Parity Rules

MSF uses **odd parity** (the total number of 1-bits in each group, including the parity bit itself, must be odd). There are four independent parity groups:

| Parity Bit | Position | Covers A-bits | Field(s) Protected |
|------------|----------|---------------|--------------------|
| P1 | 54B | 17A–24A | Year |
| P2 | 55B | 25A–35A | Month + day of month |
| P3 | 56B | 36A–38A | Day of week |
| P4 | 57B | 39A–51A | Hour + minute |

---

## 3. WWVB — United States

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | National Institute of Standards and Technology (NIST) |
| **Location** | Fort Collins, Colorado, USA (40°40′N, 105°03′W) |
| **Carrier Frequency** | 60 kHz |
| **Transmitter Power** | 70 kW |
| **Timezone** | UTC |
| **Coverage** | Continental United States (~3000 km radius) |

### Modulation Scheme

WWVB uses **reduced-carrier-first** AM modulation (like DCF77).

| Symbol | Reduction Duration | Meaning |
|--------|-------------------|---------|
| `0` | 200 ms | Binary zero |
| `1` | 500 ms | Binary one |
| Position marker | 800 ms | Frame reference (seconds 0, 9, 19, 29, 39, 49, 59) |

- **AM deviation**: ~90% (17 dB power reduction during low-power segments)
- **Encodes**: The **current minute's** time

### Time Code Encoding

WWVB uses a modified BCD encoding (`toBcdPadded5`) where each BCD digit occupies 5 bits (with unused padding bits set to 0). Values are stored **MSB first** (MSB0 format).

| Second(s) | Bits | Field | Description |
|-----------|------|-------|-------------|
| 0 | — | Position marker | Frame reference (800 ms reduction) |
| 1–8 | 8 | Minutes | BCD padded-5 (0–59), MSB first |
| 9 | — | Position marker | |
| 10–11 | 2 | Unused | Always 0 |
| 12–18 | 7 | Hours | BCD padded-5 (0–23), MSB first |
| 19 | — | Position marker | |
| 20–21 | 2 | Unused | Always 0 |
| 22–33 | 12 | Day of year | BCD padded-5 (1–366), MSB first |
| 34–35 | 2 | Unused | Always 0 |
| 36–38 | 3 | DUT1 sign | 100 = positive, 010/001 = negative |
| 39 | — | Position marker | |
| 40–43 | 4 | DUT1 magnitude | BCD (0–9) |
| 44 | 1 | Unused | Always 0 |
| 45–53 | 9 | Year | BCD padded-5 (0–99), MSB first |
| 54 | 1 | Unused | Always 0 |
| 55 | 1 | Leap year | 1 = current year is a leap year |
| 56 | 1 | Leap second | 1 = leap second at end of current month |
| 57–58 | 2 | DST status | 00 = no DST, 11 = DST active, 10/01 = transitioning |
| 59 | — | Position marker | |

### Position Markers

Position markers (800 ms reductions) occur at seconds 0, 9, 19, 29, 39, 49, and 59. They serve as frame synchronization points. Receivers use them to determine which second of the minute they are receiving.

### DST Status Encoding

| Bits 57–58 | Meaning |
|------------|---------|
| 00 | Standard time in effect, no change imminent |
| 10 | DST begins today |
| 11 | DST currently in effect |
| 01 | DST ends today |

---

## 4. JJY40 — Japan (40 kHz)

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | National Institute of Information and Communications Technology (NICT) |
| **Location** | Hagane-yama, Saga/Fukuoka, Japan (33°28′N, 130°11′E) |
| **Carrier Frequency** | 40 kHz |
| **Transmitter Power** | 50 kW |
| **Timezone** | JST (Japan Standard Time = UTC+9) |
| **Coverage** | Western Japan and surrounding regions |

### Modulation Scheme

JJY uses **full-carrier-first** AM modulation (inverted compared to DCF77/WWVB). The second starts at full power, then drops at some point during the second.

| Symbol | Full-Power Duration | Meaning |
|--------|---------------------|---------|
| `0` | 800 ms full, then reduced | Binary zero |
| `1` | 500 ms full, then reduced | Binary one |
| Position marker | 200 ms full, then reduced | Frame reference (seconds 0, 9, 19, 29, 39, 49, 59) |

- **AM deviation**: ~90% (carrier drops to ~10% during reduced portion)
- **Encodes**: The **current minute's** time

### Time Code Encoding

JJY uses two packet versions. **Version 1** (normal time data) is transmitted most minutes. **Version 2** (call sign announcement) is transmitted at minutes 15 and 45.

#### Version 1 — Time Data Packet

Uses `toBcdPadded5` BCD encoding (like WWVB), MSB first.

| Second(s) | Bits | Field | Description |
|-----------|------|-------|-------------|
| 0 | — | Position marker | Marker (200 ms full, then reduced) |
| 1–8 | 8 | Minutes | BCD padded-5 (0–59), MSB first |
| 9 | — | Position marker | |
| 10–11 | 2 | Unused | Always 0 |
| 12–18 | 7 | Hours | BCD padded-5 (0–23), MSB first |
| 19 | — | Position marker | |
| 20–21 | 2 | Unused | Always 0 |
| 22–33 | 12 | Day of year | BCD padded-5 (1–366), MSB first |
| 34–35 | 2 | Unused | Always 0 |
| 36 | 1 | PA1 | Even parity over hours (bits 12–18) |
| 37 | 1 | PA2 | Even parity over minutes (bits 1–8) |
| 38–39 | 2 | Unused | Always 0 (bit 39 is a position marker) |
| 40 | 1 | Unused | |
| 41–48 | 8 | Year | Standard BCD (0–99), MSB first |
| 49 | — | Position marker | |
| 50–52 | 3 | Day of week | BCD padded-5 (0=Sunday to 6=Saturday) |
| 53 | 1 | Leap second flag | 1 = leap second at end of UTC month |
| 54 | 1 | Leap second type | 1 = leap second added (vs. removed) |
| 55–58 | 4 | Unused | Always 0 |
| 59 | — | Position marker | |

#### Version 2 — Call Sign Announcement Packet (Minutes 15 and 45)

During minutes 15 and 45, the time data in seconds 40–48 is replaced with call sign announcement data. Additionally, during these seconds, a **Morse code** representation of "JJY" is transmitted as on/off keying instead of AM modulation.

**Morse code "JJY"**: `.--- .--- -.--`

The Morse code is stretched across seconds 40–48 (9 seconds total), and the carrier is turned fully on/off according to the Morse pattern instead of the normal AM envelope.

---

## 5. JJY60 — Japan (60 kHz)

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | National Institute of Information and Communications Technology (NICT) |
| **Location** | Ōtakadoya-yama, Fukushima, Japan (37°22′N, 140°51′E) |
| **Carrier Frequency** | 60 kHz |
| **Transmitter Power** | 50 kW |
| **Timezone** | JST (Japan Standard Time = UTC+9) |
| **Coverage** | Eastern Japan and surrounding regions |

### Signal Format

JJY60 uses **identical encoding** to JJY40. The only differences are:

- Carrier frequency: **60 kHz** (vs. 40 kHz for JJY40)
- Transmitter location: Ōtakadoya-yama, Fukushima (eastern Japan)
- Coverage area: Primarily eastern Japan

All modulation patterns, bit layouts, BCD encoding, parity rules, and call sign announcement procedures are identical to JJY40 (see section 4 above).

### Why Two Stations?

Japan operates two JJY transmitters to ensure nationwide coverage:
- **JJY40** (40 kHz): Covers western Japan from Saga/Fukuoka
- **JJY60** (60 kHz): Covers eastern Japan from Fukushima

Radio-controlled clocks in Japan typically try both frequencies to find the strongest signal. The different frequencies avoid mutual interference.

---

## 6. BPC — China

### Station Information

| Property | Value |
|----------|-------|
| **Operator** | Chinese Academy of Sciences, National Time Service Center (NTSC) |
| **Location** | Shangqiu, Henan, China (34°26′N, 115°35′E) |
| **Carrier Frequency** | 68.5 kHz |
| **Transmitter Power** | 50 kW |
| **Timezone** | CST (China Standard Time = UTC+8) |
| **Coverage** | China and surrounding regions |

### Modulation Scheme

BPC is unique among the protocols in this document: it uses **2-bit symbols** (bit pairs) per second, yielding four possible modulation levels. The carrier is reduced at the start of each second (similar to DCF77/WWVB) for a duration determined by the symbol value.

| Symbol (2 bits) | Reduction Duration | Decimal Value |
|-----------------|-------------------|---------------|
| `00` | 100 ms | 0 |
| `01` | 200 ms | 1 |
| `10` | 300 ms | 2 |
| `11` | 400 ms | 3 |

Special seconds:
- **Seconds 0, 20, 40** (reference markers): Full power for the entire second (no modulation)

- **AM deviation**: ~95%
- **Encodes**: The **current minute's** time

### Frame Structure

BPC transmits data in three 20-second frames within each minute:

| Frame | Seconds | Identifier |
|-------|---------|------------|
| Frame 0 | 0–19 | `00` |
| Frame 1 | 20–39 | `01` |
| Frame 2 | 40–59 | `10` |

Each frame contains the **same time data** (redundancy for error correction). All three frames encode identical time information.

### Symbol Layout (per 20-second frame)

Each frame contains 20 symbol positions (0–19), where each symbol is a 2-bit value.

| Position | Bits | Field | Description |
|----------|------|-------|-------------|
| 0 | 2 | Reference marker | Full power (no modulation) |
| 1 | 2 | Frame ID | `00`, `01`, or `10` for frames 0, 1, 2 |
| 2 | 2 | Unused | Always `00` |
| 3–4 | 4 | Hours (12h) | Binary (0–11), hour within 12-hour cycle |
| 5–7 | 6 | Minutes | Binary (0–59) |
| 8 (high bit) | 1 | Unused | 0 |
| 8 (low bit)–9 | 3 | Day of week | Binary (1=Monday to 7=Sunday) |
| 10 (high bit) | 1 | PM flag | 1 if hour ≥ 12 |
| 10 (low bit) | 1 | P1 parity | Even parity over symbols 1–9 |
| 11 (high bit) | 1 | Unused | 0 |
| 11–13 | 5 | Day of month | Binary (1–31) |
| 14–15 | 4 | Month | Binary (1–12) |
| 16–18 | 6 | Year (low 6 bits) | Binary, year within century |
| 19 (high bit) | 1 | Year (bit 6) | MSB of year within century |
| 19 (low bit) | 1 | P2 parity | Even parity over symbols 11–18 |

### Key Differences from Other Protocols

1. **2-bit symbols**: BPC transmits 2 bits per second (120 bits per minute vs. 59 for most others)
2. **Binary encoding**: Uses straight binary instead of BCD for most fields
3. **12-hour format**: Hours are encoded in 12-hour format with a separate PM flag
4. **Triple redundancy**: Same data transmitted in all three frames

---

## General Notes for Implementors

### Sub-Harmonic Carrier Frequencies

Real longwave frequencies (40–77.5 kHz) are far above the audible range and cannot be reproduced directly by phone speakers. Instead, the emulator uses sub-harmonics of the real carrier frequency that fall within the audible/near-ultrasonic range. The speaker's physical non-linearity generates harmonics that include the target frequency.

| Protocol | Real Freq | Sub-Harmonics Used |
|----------|-----------|-------------------|
| DCF77 | 77.5 kHz | 12916, **15500**, 19375 Hz |
| MSF | 60 kHz | 8571, **12000**, 15000 Hz |
| WWVB | 60 kHz | 8571, **12000**, 15000 Hz |
| JJY40 | 40 kHz | 5714, **8000**, 13333 Hz |
| JJY60 | 60 kHz | 8571, **12000**, 15000 Hz |
| BPC | 68.5 kHz | 11416, **13700**, 17125 Hz |

**Bold** = default carrier used by the emulator (middle frequency).

### Audio Generation

- **Sample rate**: 48000 Hz (CD-quality mono)
- **Bit depth**: 16-bit signed PCM, little-endian
- **Waveform**: Sine wave (avoids aliasing that would occur with square/triangle waves at these frequencies)
- **Transition smoothing**: 2 ms cosine ramps at amplitude transitions to prevent audible clicks
- **Playback**: Streaming mode via Android AudioTrack, blocking writes pace output to real-time

### Modulation Types

| Protocol | Modulation | Carrier During "Off" |
|----------|------------|----------------------|
| DCF77 | AM (amplitude reduction) | ~15% of full power |
| MSF | OOK (on-off keying) | 0% (carrier fully absent) |
| WWVB | AM (amplitude reduction) | ~14% of full power (-17 dB) |
| JJY | AM (amplitude reduction) | ~10% of full power |
| BPC | AM (amplitude reduction) | ~5% of full power |

### BCD Encoding Variants

Two BCD encoding methods are used across protocols:

1. **Standard BCD**: Each decimal digit → 4 binary bits (0–9). Used by DCF77 (LSB first) and MSF (MSB first).

2. **Padded-5 BCD** (`toBcdPadded5`): Each decimal digit → 5 binary bits (with bit 4 unused). Used by WWVB and JJY. This format leaves unused bit positions between digit groups, matching the real protocol's frame structure.

### Minute Encoding Direction

| Protocol | Encodes |
|----------|---------|
| DCF77 | Next minute (data becomes valid at second 0 of the following minute) |
| MSF | Next minute |
| WWVB | Current minute |
| JJY | Current minute |
| BPC | Current minute |
