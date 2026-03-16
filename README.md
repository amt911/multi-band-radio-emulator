# Multi-Band Radio Emulator

Android app that emulates longwave time signal radio stations through the device speaker. It generates amplitude-modulated audio at sub-harmonic frequencies of real broadcast stations, allowing radio-controlled clocks to synchronize without a real radio signal.

## Supported Protocols

| Station | Frequency | Location | Coverage |
| --------- | ----------- | -------------- | ---------- |
| **DCF77** | 77.5 kHz | Mainflingen, Germany | Europe |
| **MSF** | 60.0 kHz | Anthorn, United Kingdom | Europe |
| **WWVB** | 60.0 kHz | Fort Collins, USA | North America |
| **JJY40** | 40.0 kHz | Hagane-yama, Japan | Japan (West) |
| **JJY60** | 60.0 kHz | Otakadoya-yama, Japan | Japan (East) |
| **BPC** | 68.5 kHz | Shangqiu, China | China |

## How It Works

Real time signal stations broadcast at frequencies far above human hearing (40-77.5 kHz). This app generates sine waves at **sub-harmonic frequencies** (e.g., 15,500 Hz for DCF77 = 77,500 / 5). The speaker's mechanical non-linearity produces harmonics at the real carrier frequency, which radio-controlled clocks can decode.

Each protocol uses its own encoding:

- **DCF77/WWVB**: Reduced-carrier-first AM (carrier drops at the start of each second)
- **MSF**: On-off keying (carrier completely cut during modulation)
- **JJY**: Full-carrier-first AM (carrier drops after a full-power prefix)
- **BPC**: 4-level AM with 2-bit symbols (3 redundant frames per minute)

## Features

- Real-time signal generation synchronized to the system clock
- Custom date/time transmission
- Signal encoding visualization with live waveform graphs
- Adjustable signal boost for improved reception
- Material Design 3 UI with dark mode support
- Localized in English and Spanish

## Usage

1. Select a time signal station from the dropdown
2. Place your phone speaker near the radio-controlled clock's antenna
3. Press the play button
4. Wait 2-5 minutes for the clock to synchronize

> **Tip:** If the clock does not detect the signal, try increasing the signal boost in Options, and make sure the device volume is at maximum.

## Building

### Requirements

- Android Studio Ladybug or later
- JDK 11+
- Android SDK 36

### Build

```bash
./gradlew assembleDebug
```

### Release APK

```bash
./gradlew assembleRelease
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Audio**: Android AudioTrack (48 kHz, 16-bit PCM, streaming mode)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## License

This project is open source. See the repository for license details.

## Author

Andres Merlo Trujillo
