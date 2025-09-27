package com.example.tetires.data.model

/**
 * Parameter filter log (untuk search & filter riwayat).
 */
data class LogQuery(
    val searchQuery: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)
