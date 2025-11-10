package com.example.tetires.util

import com.example.tetires.model.StatusPengecekan

/**
 * Helper object untuk menentukan status ban berdasarkan ukuran tapak.
 *
 * ✅ ATURAN BISNIS (UPDATED):
 * - Ambil nilai MINIMUM dari 4 alur
 * - Jika min < 1.6 mm → AUS
 * - Jika min ≥ 1.6 mm → TIDAK AUS
 */
object TireStatusHelper {

    private const val THRESHOLD_MM = 1.6f
    private const val MIN_UKURAN = 0f
    private const val MAX_UKURAN = 20f

    /**
     * ✅ NEW: Tentukan status aus berdasarkan nilai MINIMUM dari 4 alur
     * Return true jika nilai minimum < 1.6 mm
     */
    fun isAusFromAlur(alurValues: FloatArray): Boolean {
        require(alurValues.size == 4) { "alurValues harus berisi 4 nilai" }

        // Ambil nilai minimum
        val minAlur = alurValues.minOrNull() ?: 0f

        // Aus jika minimum < 1.6mm (BUKAN <=)
        return minAlur < THRESHOLD_MM
    }

    /**
     * ✅ NEW: Tentukan status aus dengan null handling
     * Ambil nilai minimum yang valid, lalu cek < 1.6mm
     */
    fun isAusFromAlurList(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): Boolean? {
        val alurList = listOfNotNull(alur1, alur2, alur3, alur4)

        // Jika tidak ada data sama sekali
        if (alurList.isEmpty()) return null

        // Ambil nilai minimum
        val minAlur = alurList.minOrNull() ?: return null

        // Aus jika minimum < 1.6mm
        return minAlur < THRESHOLD_MM
    }

    // Validasi untuk 1 nilai alur
    fun isValidUkuran(ukuranMm: Float?): Boolean {
        return ukuranMm != null && ukuranMm in MIN_UKURAN..MAX_UKURAN
    }

    // Validasi untuk 4 nilai alur
    fun isValidAlurArray(alurValues: FloatArray): Boolean {
        return alurValues.size == 4 && alurValues.all { isValidUkuran(it) }
    }

    // Format ukuran untuk ditampilkan
    fun formatUkuran(ukuranMm: Float?): String {
        return ukuranMm?.let { "%.1f mm".format(it) } ?: "-"
    }

    // Format 4 alur untuk ditampilkan
    fun formatAlurArray(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): String {
        val values = listOf(alur1, alur2, alur3, alur4)
            .map { it?.let { "%.1f".format(it) } ?: "-" }
            .joinToString(" | ")
        return "$values mm"
    }

    /**
     * ✅ Nilai minimum dari 4 alur (untuk menentukan status)
     */
    fun getMinAlur(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): Float? {
        val values = listOfNotNull(alur1, alur2, alur3, alur4)
        return values.minOrNull()
    }

    // Format status yang ditampilkan
    fun formatStatus(isAus: Boolean?): String {
        return when (isAus) {
            true -> "Aus"
            false -> "Tidak Aus"
            null -> "Belum Dicek"
        }
    }

    /**
     * ✅ Tentukan status berdasarkan 4 alur
     */
    fun getStatusFromAlurList(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): StatusPengecekan {
        val isAus = isAusFromAlurList(alur1, alur2, alur3, alur4)
        return when (isAus) {
            null -> StatusPengecekan.BelumDicek
            true -> StatusPengecekan.Aus
            false -> StatusPengecekan.TidakAus
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

    // Validasi dan analisis 4 alur sekaligus
    fun validateAlurInput(alurValues: FloatArray): Pair<Boolean, String?> {
        return when {
            alurValues.size != 4 -> false to "Harus memasukkan 4 nilai alur"
            alurValues.any { it < MIN_UKURAN } -> false to "Nilai alur tidak boleh negatif"
            alurValues.any { it > MAX_UKURAN } -> false to "Nilai alur terlalu besar (max ${MAX_UKURAN}mm)"
            else -> true to null
        }
    }

    /**
     * ✅ NEW: Detail kondisi ban berdasarkan nilai minimum
     */
    fun getDetailStatus(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): String {
        val values = listOfNotNull(alur1, alur2, alur3, alur4)

        if (values.isEmpty()) return "Belum diukur"

        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 0f

        val isAus = minValue < THRESHOLD_MM

        return buildString {
            append("Min: ${formatUkuran(minValue)} | Max: ${formatUkuran(maxValue)} → ")
            if (isAus) append("AUS ❌")
            else append("AMAN ✅")
        }
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

    /**
     * ✅ NEW: Get teks status dengan detail threshold
     */
    fun getStatusTextWithDetail(minAlur: Float?): String {
        if (minAlur == null) return "Belum Dicek"

        return if (minAlur < THRESHOLD_MM) {
            "Aus (${formatUkuran(minAlur)} < 1.6mm)"
        } else {
            "Tidak Aus (${formatUkuran(minAlur)} ≥ 1.6mm)"
        }
    }
}