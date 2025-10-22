package com.example.tetires.data.model

/**
 * Model untuk hasil update pengecekan.
 *
 * @param complete Apakah semua 4 posisi ban sudah dicek
 * @param idCek ID pengecekan yang di-update
 * @param statusMessage Pesan status untuk user (aus/tidak aus)
 */
data class UpdateResult(
    val complete: Boolean,
    val idCek: Long,
    val statusMessage: String? = null
)