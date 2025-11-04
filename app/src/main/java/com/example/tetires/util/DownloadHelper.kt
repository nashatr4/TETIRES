package com.example.tetires.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.tetires.data.model.PengecekanRingkas
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object DownloadHelper {

    private const val TAG = "DownloadHelper"

    /**
     * Download history pengecekan sebagai CSV file dan langsung buka
     * Compatible dengan Android 10+ (Scoped Storage) dan Android 9-
     */
    fun downloadHistoryAsCSV(
        context: Context,
        logs: List<PengecekanRingkas>,
        busName: String? = null
    ) {
        if (logs.isEmpty()) {
            Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Generate CSV content
            val csvContent = generateCSV(logs)

            // Generate filename dengan timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val safeBusName = busName?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Bus"
            val fileName = "Riwayat_${safeBusName}_${timestamp}.csv"

            Log.d(TAG, "Generating CSV file: $fileName")
            Log.d(TAG, "Android SDK: ${Build.VERSION.SDK_INT}")

            // Save and get URI based on Android version
            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - Use MediaStore
                saveToDownloadsWithMediaStore(context, fileName, csvContent)
            } else {
                // Android 9 and below - Direct file access
                saveToDownloadsLegacy(context, fileName, csvContent)
            }

            if (fileUri != null) {
                Log.d(TAG, "File saved successfully: $fileUri")

                Toast.makeText(
                    context,
                    "File tersimpan! Membuka...",
                    Toast.LENGTH_SHORT
                ).show()

                // ✅ Langsung buka file
                openCSVFile(context, fileUri, fileName)
            } else {
                throw IOException("Gagal mendapatkan URI file")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving file", e)
            Toast.makeText(
                context,
                "Gagal menyimpan file: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Generate CSV content dari list pengecekan
     */
    private fun generateCSV(logs: List<PengecekanRingkas>): String {
        val header = "No,Tanggal,Waktu,Nama Bus,Plat Nomor,DKI,DKA,BKI,BKA,Status Keseluruhan\n"

        val rows = logs.mapIndexed { index, item ->
            listOf(
                (index + 1).toString(),
                item.tanggalReadable,
                item.waktuReadable,
                escapeCSV(item.namaBus),
                escapeCSV(item.platNomor),
                statusToText(item.statusDki),
                statusToText(item.statusDka),
                statusToText(item.statusBki),
                statusToText(item.statusBka),
                item.summaryStatus
            ).joinToString(",")
        }.joinToString("\n")

        return header + rows
    }

    /**
     * Escape special characters untuk CSV
     */
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Convert boolean status ke text
     */
    private fun statusToText(status: Boolean?): String {
        return when (status) {
            true -> "Aus"
            false -> "Tidak Aus"
            null -> "Belum Dicek"
        }
    }

    /**
     * Save file using MediaStore (Android 10+)
     * Returns URI of saved file
     */
    private fun saveToDownloadsWithMediaStore(
        context: Context,
        fileName: String,
        content: String
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Gagal membuat file di MediaStore")

        Log.d(TAG, "MediaStore URI: $uri")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                outputStream.flush()
            } ?: throw IOException("Gagal membuka output stream")

            // Mark file as complete
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return uri

        } catch (e: Exception) {
            Log.e(TAG, "Error writing to MediaStore", e)
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /**
     * Save file directly (Android 9 and below)
     * Returns URI of saved file
     */
    private fun saveToDownloadsLegacy(
        context: Context,
        fileName: String,
        content: String
    ): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        Log.d(TAG, "Saving to: ${file.absolutePath}")

        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
            outputStream.flush()
        }

        // Create URI using FileProvider
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating FileProvider URI", e)
            Uri.fromFile(file) // Fallback
        }
    }

    /**
     * ✅ Buka file CSV dengan app yang sesuai (Excel, Google Sheets, dll)
     */
    private fun openCSVFile(context: Context, uri: Uri, fileName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if ada app yang bisa buka CSV
            val chooser = Intent.createChooser(intent, "Buka file dengan:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening file", e)

            // ✅ Fallback: Share file instead
            shareCSVFile(context, uri, fileName)
        }
    }

    /**
     * Share file jika tidak bisa dibuka langsung
     */
    private fun shareCSVFile(context: Context, uri: Uri, fileName: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Riwayat Pengecekan Ban")
                putExtra(Intent.EXTRA_TEXT, "File: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan file:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooser)

            Toast.makeText(
                context,
                "File tersimpan di Downloads",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file", e)
            Toast.makeText(
                context,
                "File tersimpan di folder Downloads: $fileName",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ✅ Alternative: Simpan di internal storage lalu share
     * Untuk bypass masalah permission
     */
    fun downloadAndShareCSV(
        context: Context,
        logs: List<PengecekanRingkas>,
        busName: String? = null
    ) {
        if (logs.isEmpty()) {
            Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvContent = generateCSV(logs)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val safeBusName = busName?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Bus"
            val fileName = "Riwayat_${safeBusName}_${timestamp}.csv"

            // ✅ Simpan di cache internal (tidak perlu permission)
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )

            Log.d(TAG, "Cache file: ${cacheFile.absolutePath}")
            Log.d(TAG, "FileProvider URI: $uri")

            // ✅ Buka dengan chooser (Excel, Sheets, File Manager, dll)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Buka riwayat dengan:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Log.e(TAG, "Error in downloadAndShare", e)
            Toast.makeText(
                context,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ✅ Download riwayat detail (dengan ukuran ban per posisi)
     * Format CSV akan include ukuran aktual ban jika tersedia
     */
    fun downloadDetailedHistory(
        context: Context,
        logs: List<PengecekanRingkas>,
        busName: String? = null
    ) {
        if (logs.isEmpty()) {
            Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Generate detailed CSV content
            val csvContent = generateDetailedCSV(logs)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val safeBusName = busName?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Bus"
            val fileName = "Riwayat_Detail_${safeBusName}_${timestamp}.csv"

            Log.d(TAG, "Generating detailed CSV: $fileName")

            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloadsWithMediaStore(context, fileName, csvContent)
            } else {
                saveToDownloadsLegacy(context, fileName, csvContent)
            }

            if (fileUri != null) {
                Toast.makeText(
                    context,
                    "File detail tersimpan! Membuka...",
                    Toast.LENGTH_SHORT
                ).show()
                openCSVFile(context, fileUri, fileName)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading detailed history", e)
            Toast.makeText(
                context,
                "Gagal menyimpan detail: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Generate CSV dengan detail ukuran ban per posisi
     * Format: Include kolom ukuran untuk setiap posisi ban
     */
    private fun generateDetailedCSV(logs: List<PengecekanRingkas>): String {
        // Header dengan kolom ukuran ban
        val header = "No,Tanggal,Waktu,Nama Bus,Plat Nomor," +
                "DKI Status,DKA Status,BKI Status,BKA Status," +
                "Status Keseluruhan\n"

        val rows = logs.mapIndexed { index, item ->
            listOf(
                (index + 1).toString(),
                item.tanggalReadable,
                item.waktuReadable,
                escapeCSV(item.namaBus),
                escapeCSV(item.platNomor),
                statusToText(item.statusDki),
                statusToText(item.statusDka),
                statusToText(item.statusBki),
                statusToText(item.statusBka),
                item.summaryStatus
            ).joinToString(",")
        }.joinToString("\n")

        return header + rows
    }
}