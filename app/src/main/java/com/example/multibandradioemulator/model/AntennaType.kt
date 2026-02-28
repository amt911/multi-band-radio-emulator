package com.example.multibandradioemulator.model

import androidx.annotation.StringRes
import com.example.multibandradioemulator.R

/**
 * Supported radio-controlled clock signal protocols.
 * Each antenna type corresponds to a real longwave time signal station.
 */
enum class AntennaType(
    val displayName: String,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val regionRes: Int,
    val frequencyKHz: Double
) {
    DCF77(
        displayName = "DCF77",
        descriptionRes = R.string.dcf77_description,
        regionRes = R.string.dcf77_region,
        frequencyKHz = 77.5
    ),
    WWVB(
        displayName = "WWVB",
        descriptionRes = R.string.wwvb_description,
        regionRes = R.string.wwvb_region,
        frequencyKHz = 60.0
    ),
    JJY(
        displayName = "JJY",
        descriptionRes = R.string.jjy_description,
        regionRes = R.string.jjy_region,
        frequencyKHz = 40.0 // JJY transmits on 40 kHz and 60 kHz
    ),
    BPC(
        displayName = "BPC",
        descriptionRes = R.string.bpc_description,
        regionRes = R.string.bpc_region,
        frequencyKHz = 68.5
    );
}
