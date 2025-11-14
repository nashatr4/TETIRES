package com.example.tetires.data.model

data class AlurBan(
    val alur1: Float?,
    val alur2: Float?,
    val alur3: Float?,
    val alur4: Float?
) {

    val formattedAlur1: String
        get() = alur1?.let { "%.3f".format(it) } ?: "N/A"

    val formattedAlur2: String
        get() = alur2?.let { "%.3f".format(it) } ?: "N/A"

    val formattedAlur3: String
        get() = alur3?.let { "%.3f".format(it) } ?: "N/A"

    val formattedAlur4: String
        get() = alur4?.let { "%.3f".format(it) } ?: "N/A"

    val minAlur: Float?
        get() = listOfNotNull(alur1, alur2, alur3, alur4).minOrNull()

    val formattedMinAlur: String
        get() = minAlur?.let { "%.3f mm".format(it) } ?: "N/A"

    data class AlurDisplay(
        val label: String,
        val value: Float?,
        val isWorn: Boolean,
        val isMissing: Boolean
    )

    fun getFormattedAlurList(): List<AlurDisplay> {
        return listOf(
            AlurDisplay("Alur 1", alur1, alur1 != null && alur1 < 1.6f, alur1 == null),
            AlurDisplay("Alur 2", alur2, alur2 != null && alur2 < 1.6f, alur2 == null),
            AlurDisplay("Alur 3", alur3, alur3 != null && alur3 < 1.6f, alur3 == null),
            AlurDisplay("Alur 4", alur4, alur4 != null && alur4 < 1.6f, alur4 == null)
        )
    }

}