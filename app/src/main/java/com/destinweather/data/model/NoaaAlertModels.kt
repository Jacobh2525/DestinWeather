package com.destinweather.data.model

data class NoaaAlertResponse(
    val features: List<AlertFeature>?
)

data class AlertFeature(
    val properties: AlertProperties?
)

data class AlertProperties(
    val id: String?,
    val event: String?,           // e.g., "Tornado Warning"
    val headline: String?,       // Short headline
    val description: String?,     // Full description
    val instruction: String?,     // Safety instructions
    val severity: String?,       // "Extreme", "Severe", "Moderate", "Minor"
    val certainty: String?,      // "Likely", "Possible", etc.
    val urgency: String?,        // "Immediate", "Expected", "Future"
    val areaDesc: String?,       // Area description
    val effective: String?,      // When it starts
    val expires: String?,        // When it ends
    val sender: String?          // Who issued it (NWS)
)
