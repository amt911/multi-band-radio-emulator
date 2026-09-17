package com.example.multibandradioemulator.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.multibandradioemulator.ui.theme.MultiBandRadioEmulatorTheme

// Compose Preview Screenshot Testing (host-side, LayoutLib) previews for two of this app's
// existing screens (each already has its own @Preview in main — AntennaInfoScreen.kt,
// OptionsScreen.kt). @PreviewTest is only legal in the screenshotTest source set, so these
// mirror those existing previews here rather than annotating the originals.
//
// HomeScreen is deliberately NOT included here: it seeds its clock from
// `mutableStateOf(LocalDateTime.now())` with no injectable clock parameter, so its render is
// wall-clock-dependent — `updateDebugScreenshotTest` and `validateDebugScreenshotTest` run a few
// seconds apart and render two different times, which fails validation on every run regardless
// of any real UI change (confirmed: validate failed against the reference update had just
// produced). Screenshotting it would need a clock seam added to the composable itself, which is
// a production-code change beyond this task's tooling/config scope.
//
// Generate references: ./gradlew updateDebugScreenshotTest
// Verify (fails on visual drift):  ./gradlew validateDebugScreenshotTest

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun AntennaInfoScreenScreenshotTest() {
    MultiBandRadioEmulatorTheme {
        AntennaInfoScreen()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
private fun OptionsScreenScreenshotTest() {
    MultiBandRadioEmulatorTheme {
        OptionsScreen()
    }
}
