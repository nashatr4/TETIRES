package com.example.tetires.util

import com.example.tetires.ui.screen.StatusPengecekan

/**
 * Helper object untuk menentukan status ban berdasarkan ukuran tapak.
 *
 * Aturan:
 * - ukuran > 1.6 mm = TIDAK AUS
 * - ukuran <= 1.6 mm = AUS
 */
object TireStatusHelper {

    private const val THRESHOLD_MM = 1.6f

    /**
     * Menentukan apakah ban aus berdasarkan ukuran tapak.
     *
     * @param ukuranMm Ukuran tapak ban dalam milimeter
     * @return true jika aus (<=1.6mm), false jika tidak aus (>1.6mm)
     *         null jika ukuranMm null
     */
    fun isAus(ukuranMm: Float?): Boolean? {
        return ukuranMm?.let { it <= THRESHOLD_MM }
    }

    /**
     * Validasi ukuran tapak ban.
     *
     * @param ukuranMm Ukuran yang akan divalidasi
     * @return true jika valid, false jika tidak
     */
    fun isValidUkuran(ukuranMm: Float?): Boolean {
        return ukuranMm != null && ukuranMm in 0f..50f
    }

    /**
     * Format ukuran untuk ditampilkan.
     */
    fun formatUkuran(ukuranMm: Float?): String {
        return ukuranMm?.let { "%.1f mm".format(it) } ?: "-"
    }

    /**
     * Format status untuk ditampilkan.
     */
    fun formatStatus(isAus: Boolean?): String {
        return when (isAus) {
            true -> "Aus"
            false -> "Tidak Aus"
            null -> "Belum Dicek"
        }
    }

    /**
     * Tentukan status otomatis berdasarkan ukuran, dengan default jika null
     */
    fun getStatusFromUkuran(ukuranMm: Float?): StatusPengecekan {
        return when {
            ukuranMm == null -> StatusPengecekan.BelumDicek
            ukuranMm <= THRESHOLD_MM -> StatusPengecekan.Aus
            else -> StatusPengecekan.TidakAus
        }
    }
}
