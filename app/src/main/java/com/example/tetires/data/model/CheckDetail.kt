package com.example.tetires.data.model

data class CheckDetail(
    val idCek: Long,
    val tanggalCek: Long,
    val tanggalReadable: String,
    val waktuReadable: String,
    val namaBus: String,
    val platNomor: String,
    val statusDka: Boolean,
    val statusDki: Boolean,
    val statusBka: Boolean,
    val statusBki: Boolean,
    val alurDka: AlurBan?,
    val alurDki: AlurBan?,
    val alurBka: AlurBan?,
    val alurBki: AlurBan?
)