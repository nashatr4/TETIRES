package com.example.tetires.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val DEFAULT_PATTERN = "dd MMMM yyyy"
    private val localeID: Locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    private val timeZoneJakarta = TimeZone.getTimeZone("Asia/Jakarta")
    /**
     * Convert epoch millis ke string tanggal (format: dd MMMM yyyy, ex: 29 April 2025)
     */
    fun formatDate(epochMillis: Long, pattern: String = DEFAULT_PATTERN): String {
        val sdf = SimpleDateFormat(pattern, localeID)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", localeID)
        sdf.timeZone = timeZoneJakarta
        return sdf.format(Date(epochMillis))
    }

    /**
     * Ambil epoch millis untuk jam 00:00:00 dari tanggal tertentu
     */
    fun getStartOfDay(date: Date = Date()): Long {
        val calendar = Calendar.getInstance(timeZoneJakarta)
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Ambil epoch millis untuk jam 23:59:59 dari tanggal tertentu
     */
    fun getEndOfDay(date: Date = Date()): Long {
        val calendar = Calendar.getInstance(timeZoneJakarta)
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Ambil epoch millis waktu sekarang (hari ini)
     */
    fun today(): Long = System.currentTimeMillis()
}
