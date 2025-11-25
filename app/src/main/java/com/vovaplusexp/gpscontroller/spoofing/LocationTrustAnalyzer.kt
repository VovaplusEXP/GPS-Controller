package com.vovaplusexp.gpscontroller.spoofing

data class LocationTrustAnalyzer(
    val trustLevel: TrustLevel,
    val flags: List<GpsSpoofingDetector.SpoofingFlag>,
    val confidence: Float
) {
    enum class TrustLevel {
        TRUSTED,
        SUSPICIOUS,
        SPOOFED
    }

    val description: String
        get() = flags.joinToString(", ") { it.name }

    fun isTrusted(): Boolean = trustLevel == TrustLevel.TRUSTED
    fun isSpoofed(): Boolean = trustLevel == TrustLevel.SPOOFED
}
