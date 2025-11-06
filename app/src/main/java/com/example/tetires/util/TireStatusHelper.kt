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
    private const val MIN_UKURAN = 0f
    private const val MAX_UKURAN = 20f

    // Return true jika ada minimal 1 alur <= 1.6 mm
    fun isAusFromAlur(alurValues: FloatArray): Boolean {
        require(alurValues.size == 4) { "alurValues harus berisi 4 nilai" }
        // Aus jika ada minimal 1 alur <= 1.6mm
        return alurValues.any { it <= THRESHOLD_MM }
    }

    // Tentukan status aus berdasarkan list alur (dengan null handling)
    fun isAusFromAlurList(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): Boolean? {
        val alurList = listOfNotNull(alur1, alur2, alur3, alur4)

        // Jika tidak ada data sama sekali
        if (alurList.isEmpty()) return null

        // Aus jika ada minimal 1 alur ≤ 1.6mm
        return alurList.any { it <= THRESHOLD_MM }
    }

    @Deprecated(
        message = "Gunakan isAusFromAlur() untuk logika baru (4 alur)",
        replaceWith = ReplaceWith("isAusFromAlurList(ukuranMm, ukuranMm, ukuranMm, ukuranMm)")
    )
    fun isAus(ukuranMm: Float?): Boolean? {
        return ukuranMm?.let { it <= THRESHOLD_MM }
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

    // Nilai minimum dari 4 alur
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

    // Gunakan isAusFromAlurList()
    @Deprecated(
        message = "Gunakan isAusFromAlurList() untuk logika baru",
        replaceWith = ReplaceWith("getStatusFromAlurList(alur1, alur2, alur3, alur4)")
    )
    fun getStatusFromUkuran(ukuranMm: Float?): StatusPengecekan {
        return when {
            ukuranMm == null -> StatusPengecekan.BelumDicek
            ukuranMm <= THRESHOLD_MM -> StatusPengecekan.Aus
            else -> StatusPengecekan.TidakAus
        }
    }

    // Tentukan status berdasarkan 4 alur
    fun getStatusFromAlurList(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): StatusPengecekan {
        val isAus = isAusFromAlurList(alur1, alur2, alur3, alur4)
        return when (isAus) {
            null -> StatusPengecekan.BelumDicek
            true -> StatusPengecekan.Aus
            false -> StatusPengecekan.TidakAus
        }
    }

    fun summaryStatus(
        dka: Boolean?, dki: Boolean?,
        bka: Boolean?, bki: Boolean?
    ): String {
        val list = listOf(dka, dki, bka, bki)
        return when {
            list.any { it == null } -> "Belum Selesai"
            list.any { it == true } -> "Aus"
            else -> "Tidak Aus"
        }
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

    // Detail kondisi ban berdasarkan 4 alur
    fun getDetailStatus(alur1: Float?, alur2: Float?, alur3: Float?, alur4: Float?): String {
        val values = listOfNotNull(alur1, alur2, alur3, alur4)

        if (values.isEmpty()) return "Belum diukur"

        val ausCount = values.count { it <= THRESHOLD_MM }
        val minValue = values.minOrNull() ?: 0f

        return when {
            ausCount == 0 -> "Semua alur aman (>1.6mm)"
            ausCount == values.size -> "Semua alur aus (≤1.6mm)"
            else -> "$ausCount dari ${values.size} alur aus (min: ${formatUkuran(minValue)})"
        }
    }

}
