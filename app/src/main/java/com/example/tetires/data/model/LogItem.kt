package com.example.tetires.data.model

/**
 * Data sederhana untuk ditampilkan di layar Beranda / Riwayat.
 */
// File: data/model/LogItem.kt
data class LogItem(
    val idCek: Long,
    val tanggalCek: Long,
    val tanggalReadable: String,
    val waktuReadable: String,
    val namaBus: String,
    val platNomor: String,
    val summaryStatus: String
)
