package com.example.tetires.util

/**
 * Helper object untuk menentukan status ban berdasarkan ukuran tapak.
 *
 * Aturan:
 * - ukuran > 1.6 mm = TIDAK AUS (false)
 * - ukuran <= 1.6 mm = AUS (true)
 */
object TireStatusHelper {

    private const val THRESHOLD_MM = 1.6f

    /**
     * Menentukan apakah ban aus berdasarkan ukuran tapak.
     *
     * @param ukuranMm Ukuran tapak ban dalam milimeter
     * @return true jika aus (<=1.6mm), false jika tidak aus (>1.6mm)
     */
    fun isAus(ukuranMm: Float?): Boolean? {
        return ukuranMm?.let { it <= THRESHOLD_MM }
    }

    /**
     * Validasi ukuran tapak ban (harus positif dan masuk akal).
     *
     * @param ukuranMm Ukuran yang akan divalidasi
     * @return true jika valid, false jika tidak
     */
    fun isValidUkuran(ukuranMm: Float?): Boolean {
        return ukuranMm != null && ukuranMm >= 0f && ukuranMm <= 50f // max 50mm sebagai batas wajar
    }

    /**
     * Format ukuran untuk ditampilkan.
     */
    fun formatUkuran(ukuranMm: Float?): String {
        return ukuranMm?.let { "%.1f mm" .format(it) } ?: "-"
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
}