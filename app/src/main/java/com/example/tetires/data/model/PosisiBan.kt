package com.example.tetires.data.model

/**
 * Posisi ban pada bus
 */
enum class PosisiBan(val label: String) {
    DKA("D-KA"), // Depan Kanan
    DKI("D-KI"), // Depan Kiri
    BKA("B-KA"), // Belakang Kanan
    BKI("B-KI"); // Belakang Kiri

    companion object {
        /**
         * Konversi string ke enum
         * Misal "D-KA" -> PosisiBan.DKA
         */
        fun fromString(value: String): PosisiBan? {
            return values().firstOrNull { it.label.equals(value, ignoreCase = true) }
        }
    }
}
