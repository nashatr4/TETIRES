package com.example.tetires.data.local.entity

/**
 * Projection untuk hasil JOIN tabel pengecekan + bus (khusus untuk query getPengecekanByBus).
 * Ini bukan entity, hanya data class untuk query result.
 */
data class PengecekanByBusWithBus(
    val idPengecekan: Long,
    val tanggalMs: Long,
    val statusDka: Boolean?,
    val statusDki: Boolean?,
    val statusBka: Boolean?,
    val statusBki: Boolean?,
    val namaBus: String,
    val platNomor: String
)
