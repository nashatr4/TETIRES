package com.example.tetires.data.model

/**
 * ✅ Model untuk list riwayat pengecekan (summary)
 * Dipakai di RiwayatScreen dan BerandaScreen
 */
data class PengecekanRingkas(
    val idCek: Long,  // ✅ KONSISTEN: Pakai idCek
    val tanggalCek: Long,
    val tanggalReadable: String,
    val waktuReadable: String,
    val namaBus: String,
    val platNomor: String,
    val statusDka: Boolean?,
    val statusDki: Boolean?,
    val statusBka: Boolean?,
    val statusBki: Boolean?,
    val summaryStatus: String
)