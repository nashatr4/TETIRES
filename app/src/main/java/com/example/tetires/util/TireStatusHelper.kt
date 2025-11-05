package com.example.tetires.util

import com.example.tetires.model.StatusPengecekan

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

    fun summaryStatus(
        dka: Boolean?,
        dki: Boolean?,
        bka: Boolean?,
        bki: Boolean?
    ): String {
        val list = listOf(dka, dki, bka, bki)

        // ⏳ Jika ada yang null → belum selesai
        if (list.any { it == null }) {
            return "Belum Selesai"
        }

        // ❌ Jika ada minimal 1 ban aus (true) → AUS
        if (list.any { it == true }) {
            return "Aus"
        }

        // ✅ Jika SEMUA ban tidak aus (semua false) → TIDAK AUS
        return "Tidak Aus"
    }
    fun getStatusColor(isAus: Boolean?): Long {
        return when (isAus) {
            true -> 0xFFEF4444  // Merah
            false -> 0xFF10B981 // Hijau
            else -> 0xFF6B7280  // Abu-abu
        }
    }

    /**
     * Get teks status
     */
    fun getStatusText(isAus: Boolean?): String {
        return when (isAus) {
            true -> "Aus"
            false -> "Tidak Aus"
            else -> "Belum Dicek"
        }
    }
}