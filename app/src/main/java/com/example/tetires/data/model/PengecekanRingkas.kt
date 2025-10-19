package com.example.tetires.data.model

data class PengecekanRingkas(
    val idCek: Long,
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

