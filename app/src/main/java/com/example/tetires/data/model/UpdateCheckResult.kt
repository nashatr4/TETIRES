package com.example.tetires.data.model

/**
 * Result object untuk updateCheckPartial
 * Letakkan file ini di: app/src/main/java/com/example/tetires/data/model/UpdateCheckResult.kt
 */
data class UpdateCheckResult(
    val complete: Boolean,        // Apakah semua ban sudah dicek?
    val statusMessage: String?    // Pesan status untuk user
)