package com.example.multibandradioemulator.model

/**
 * Supported radio-controlled clock signal protocols.
 * Each antenna type corresponds to a real longwave time signal station.
 */
enum class AntennaType(
    val displayName: String,
    val description: String,
    val region: String,
    val frequencyKHz: Double
) {
    DCF77(
        displayName = "DCF77",
        description = "Mainflingen, Alemania",
        region = "Europa",
        frequencyKHz = 77.5
    ),
    WWVB(
        displayName = "WWVB",
        description = "Fort Collins, EE.UU.",
        region = "Norteamérica",
        frequencyKHz = 60.0
    ),
    JJY(
        displayName = "JJY",
        description = "Fukushima/Saga, Japón",
        region = "Asia (Japón)",
        frequencyKHz = 40.0 // JJY transmits on 40 kHz and 60 kHz
    ),
    BPC(
        displayName = "BPC",
        description = "Shangqiu, China",
        region = "Asia (China)",
        frequencyKHz = 68.5
    );

    fun formattedLabel(): String = "$displayName — $region ($frequencyKHz kHz)"
}
