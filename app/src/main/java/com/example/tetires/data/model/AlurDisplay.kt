package com.example.tetires.data.model

data class AlurDisplay(
    val label: String,
    val value: Float?
) {
    val formattedValue: String
        get() = value?.let { "%.1f mm".format(it) } ?: "N/A"

    val isWorn: Boolean
        get() = value != null && value < 1.6f

    val isMissing: Boolean
        get() = value == null
}