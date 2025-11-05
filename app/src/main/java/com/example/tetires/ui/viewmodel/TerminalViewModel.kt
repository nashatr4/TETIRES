package com.example.tetires.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.example.tetires.util.BluetoothHelper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TerminalViewModel(private val context: Context) : ViewModel() {

    var terminalText = mutableStateOf("")
    var lastCheck = mutableStateOf("")

    private val lineBuffer = mutableListOf<String>()
    private val bluetoothHelper = BluetoothHelper(context)
    private val pythonInstance: Python = Python.getInstance()
    private val filteringModule: PyObject = pythonInstance.getModule("filtering")

    private var lastClearedLog = ""

    init {
        // --- 2. Hubungkan Callback BluetoothHelper ---
        // Panggil fungsi Python saat data diterima
        bluetoothHelper.onDataReceived = { rawData ->
            processAndLogData(rawData)
        }

        // Tampilkan status sistem di log
        bluetoothHelper.onStatusChange = { statusText ->
            addLog("SYSTEM: $statusText")
        }
    }

    private fun processAndLogData(rawData: String) {
        addLog(rawData)
        lineBuffer.add(rawData)
    }

    fun processBufferWithPython() {
        if (lineBuffer.isEmpty()) {
            addLog("PYTHON: Tidak ada data di buffer untuk diproses.")
            return
        }

        addLog("PYTHON: Memulai pemrosesan batch... (ini mungkin butuh beberapa detik)")

        // Salin buffer agar aman dari perubahan saat proses
        // dan bersihkan buffer lama
        val dataToProcess = ArrayList(lineBuffer)
        lineBuffer.clear()

        viewModelScope.launch(Dispatchers.Default) { // Jalankan di thread background
            try {
                // Dapatkan path penyimpanan internal aplikasi
                val storagePath = context.filesDir.absolutePath

                // 5. Panggil Fungsi Python dengan seluruh buffer
                // Ini memanggil: process_data_batch(dataToProcess, storagePath)
                val resultPyObject = filteringModule.callAttr("process_data_batch", dataToProcess, storagePath)

                // 6. Konversi hasil (angka float) ke String
                val meanResult = resultPyObject.toFloat()
                val resultString = String.format(Locale.US, "%.4f", meanResult)

                // 6. Tampilkan hasil ringkasan dari Python
                withContext(Dispatchers.Main) {
                    addLog("HASIL AKHIR (Mean Voltage): $resultString mV")
                    addLog("(Plot disimpan di penyimpanan internal aplikasi)")
                }

            } catch (e: Exception) {
                Log.e("TerminalViewModel", "Error memanggil Python: ${e.message}")
                withContext(Dispatchers.Main) {
                    addLog("PYTHON ERROR: ${e.message}")
                }
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun connectBluetooth() {
        // Kosongkan buffer dan log lama setiap kali koneksi baru
        clearLogs()
        lineBuffer.clear()
        addLog("SYSTEM: Mencoba menghubungkan...")
        bluetoothHelper.connect()
    }

    fun disconnectBluetooth() {
        bluetoothHelper.disconnect()
    }

    fun sendBluetoothCommand(command: String) {
        bluetoothHelper.send(command)
        addLog("KOTLIN (SENT): $command") // Tampilkan perintah terkirim di log
    }

    fun addLog(text: String) {
        val timestamp = getCurrentTimestamp()
        terminalText.value += "\n[$timestamp] $text"
    }

    fun clearLogs() {
        if (terminalText.value.isNotEmpty()) {
            lastClearedLog = terminalText.value
            lastCheck.value = lastClearedLog
            terminalText.value = ""
        }
    }

    fun restoreLastLogs() {
        if (lastClearedLog.isNotEmpty()) {
            terminalText.value = lastClearedLog
            lastClearedLog = "" // reset biar tombol undo gak terus muncul
        }
    }
}
