import com.example.tetires.data.model.AlurDisplay


data class AlurBan(
    val alur1: Float?,
    val alur2: Float?,
    val alur3: Float?,
    val alur4: Float?
) {
    // Nilai minimum dari 4 alur (untuk menentukan status)
    val minAlur: Float?
        get() = listOfNotNull(alur1, alur2, alur3, alur4).minOrNull()

    // Nilai maksimum dari 4 alur
    val maxAlur: Float?
        get() = listOfNotNull(alur1, alur2, alur3, alur4).maxOrNull()

    // Rata-rata dari 4 alur
    val avgAlur: Float?
        get() {
            val values = listOfNotNull(alur1, alur2, alur3, alur4)
            return if (values.isNotEmpty()) values.average().toFloat() else null
        }

    // Label alur yang terkecil (untuk display)
    val minAlurLabel: String
        get() {
            val values = listOf(
                "Alur 1" to alur1,
                "Alur 2" to alur2,
                "Alur 3" to alur3,
                "Alur 4" to alur4
            )
            val minEntry = values.filter { it.second != null }
                .minByOrNull { it.second ?: Float.MAX_VALUE }
            return minEntry?.first ?: "N/A"
        }

    // Format untuk display di UI
    val formattedMinAlur: String
        get() = minAlur?.let { "%.1f mm".format(it) } ?: "N/A"

    val formattedMaxAlur: String
        get() = maxAlur?.let { "%.1f mm".format(it) } ?: "N/A"

    val formattedAvgAlur: String
        get() = avgAlur?.let { "%.1f mm".format(it) } ?: "N/A"

    // Cek apakah semua alur valid (tidak null)
    val isComplete: Boolean
        get() = alur1 != null && alur2 != null && alur3 != null && alur4 != null

    // Jumlah alur yang sudah diukur
    val measuredCount: Int
        get() = listOfNotNull(alur1, alur2, alur3, alur4).size

    // Convert ke array untuk processing
    fun toArray(): FloatArray {
        return floatArrayOf(
            alur1 ?: 0f,
            alur2 ?: 0f,
            alur3 ?: 0f,
            alur4 ?: 0f
        )
    }

    // Format semua alur untuk display
    fun getFormattedAlurList(): List<AlurDisplay> {
        return listOf(
            AlurDisplay("Alur 1", alur1),
            AlurDisplay("Alur 2", alur2),
            AlurDisplay("Alur 3", alur3),
            AlurDisplay("Alur 4", alur4)
        )
    }
}
