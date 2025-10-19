package com.example.tetires.data.local.entity

/**
 * Projection untuk hasil JOIN tabel pengecekan + bus.
 * Ini bukan entity, hanya data class untuk query result.
 */
data class PengecekanWithBus(
    val idPengecekan: Long,
    val tanggalMs: Long,
    val waktuMs: Long,
    val statusDka: Boolean?,
    val statusDki: Boolean?,
    val statusBka: Boolean?,
    val statusBki: Boolean?,
    val namaBus: String,
    val platNomor: String
)
