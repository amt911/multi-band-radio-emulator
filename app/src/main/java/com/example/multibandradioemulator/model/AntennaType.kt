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
    MSF(
        displayName = "MSF",
        descriptionRes = R.string.msf_description,
        regionRes = R.string.msf_region,
        frequencyKHz = 60.0
    ),
    WWVB(
        displayName = "WWVB",
        descriptionRes = R.string.wwvb_description,
        regionRes = R.string.wwvb_region,
        frequencyKHz = 60.0
    ),
    JJY40(
        displayName = "JJY40",
        descriptionRes = R.string.jjy40_description,
        regionRes = R.string.jjy40_region,
        frequencyKHz = 40.0
    ),
    JJY60(
        displayName = "JJY60",
        descriptionRes = R.string.jjy60_description,
        regionRes = R.string.jjy60_region,
        frequencyKHz = 60.0
    ),
    BPC(
        displayName = "BPC",
        descriptionRes = R.string.bpc_description,
        regionRes = R.string.bpc_region,
        frequencyKHz = 68.5
    );
}
