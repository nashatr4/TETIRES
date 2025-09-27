package com.example.tetires.data.model

/**
 * Projection hasil JOIN antara Pengecekan dan Bus.
 * Dipakai untuk menampilkan pengecekan dengan info bus.
 */
data class CheckWithBusProjection(
    val idCek: Long,
    val tanggalCek: Long,
    val namaBus: String,
    val platNomor: String,
    val statusDka: Boolean?,
    val statusDki: Boolean?,
    val statusBka: Boolean?,
    val statusBki: Boolean?,
    val isComplete: Boolean
)
