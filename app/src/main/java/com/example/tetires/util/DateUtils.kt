package com.example.tetires.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // ✅ Timezone WIB (Jakarta)
    private val wibTimeZone = TimeZone.getTimeZone("Asia/Jakarta")

    private val localeID = Locale.forLanguageTag("id-ID")

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", localeID).apply {
        timeZone = wibTimeZone
    }

    private val timeFormat = SimpleDateFormat("HH:mm", localeID).apply {
        timeZone = wibTimeZone
    }

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", localeID).apply {
        timeZone = wibTimeZone
    }

    /**
     * Format timestamp ke tanggal saja (WIB)
     * Contoh: "22 Okt 2025"
     */
    fun formatDate(timestampMs: Long): String {
        return try {
            dateFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Format timestamp ke waktu saja (WIB)
     * Contoh: "10:30"
     */
    fun formatTime(timestampMs: Long): String {
        return try {
            timeFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            "Invalid Time"
        }
    }

    /**
     * Format timestamp ke tanggal dan waktu (WIB)
     * Contoh: "22 Okt 2025 10:30"
     */
    fun formatDateTime(timestampMs: Long): String {
        return try {
            dateTimeFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            "Invalid DateTime"
        }
    }

    /**
     * Get timestamp sekarang (dalam WIB)
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Convert tanggal string ke timestamp (WIB)
     */
    fun parseDate(dateString: String): Long? {
        return try {
            dateFormat.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get waktu sekarang dalam format readable (WIB)
     * Contoh: "22 Okt 2025 14:30"
     */
    fun getCurrentDateTimeReadable(): String {
        return formatDateTime(getCurrentTimestamp())
    }
}