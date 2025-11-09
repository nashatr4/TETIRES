package com.example.tetires.data.model

import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.PengukuranAlur
import AlurBan
import com.example.tetires.data.model.PosisiBan
import com.example.tetires.util.DateUtils

/**
 * Data detail pengecekan lengkap (untuk ditampilkan di UI detail)
 */
data class CheckDetail(
    val idCek: Int,
    val tanggal: Long,
    val namaBus: String,
    val platNomor: String,
    val posisiBan: PosisiBan,
    val alurBan: AlurBan,
    val detailBan: DetailBan?,
    val pengukuran: List<PengukuranAlur>?
) {
    val tanggalReadable: String
        get() = DateUtils.formatDateOnly(tanggal)

    val waktuReadable: String
        get() = DateUtils.formatTimeOnly(tanggal)

    val statusBan: String
        get() {
            val min = alurBan.minAlur
            return when {
                min == null -> "Belum Diperiksa"
                min < 1.6f -> "Aus"
                else -> "Baik"
            }
        }
}
