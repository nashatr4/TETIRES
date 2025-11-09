package com.example.tetires.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))

    /**
     * Format timestamp ke tanggal saja
     * Contoh: "22 Okt 2025"
     */
    fun formatDateOnly(timestampMs: Long): String {
        return try {
            dateFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Format timestamp ke waktu saja
     * Contoh: "10:30"
     */
    fun formatTimeOnly(timestampMs: Long): String {
        return try {
            timeFormat.format(Date(timestampMs))
        } catch (e: Exception) {
            "Invalid Time"
        }
    }

    /**
     * Format timestamp ke tanggal dan waktu
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
     * Get timestamp sekarang
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Convert tanggal string ke timestamp
     */
    fun parseDate(dateString: String): Long? {
        return try {
            dateFormat.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
}