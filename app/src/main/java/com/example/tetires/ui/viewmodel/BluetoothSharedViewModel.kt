package com.example.tetires.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.model.PosisiBan
import com.example.tetires.util.BluetoothHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * State untuk proses CekBan
 */
enum class CekBanState {
    IDLE,              // Belum mulai, belum pilih posisi
    WAITING_SCAN,      // User sudah pilih posisi, siap scan
    SCANNING,          // Sedang scan (terima data)
    PROCESSING,        // Processing data dengan Python
    RESULT_READY,      // Hasil sudah keluar, belum disimpan
    SAVED,             // Sudah disimpan ke DB
    ERROR              // Terjadi error
}

/**
 * Mode operasi aplikasi
 */
enum class AppMode {
    TERMINAL,  // Mode terminal (free, tidak ada konteks)
    CEK_BAN    // Mode cek ban (guided, ada konteks bus & posisi)
}

/**
 * Shared ViewModel untuk Terminal dan CekBan
 */
class BluetoothSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BluetoothSharedVM"

    // Database
    private val database = AppDatabase.getInstance(application)
    private val pengecekanDao = database.pengecekanDao()
    private val detailBanDao = database.detailBanDao()
    private val pengukuranAlurDao = database.pengukuranAlurDao()

    // Bluetooth Helper (single instance)
    private val bluetoothHelper = BluetoothHelper(application)

    // Python instance
    private val pythonInstance: Python = Python.getInstance()
    private val processingModule by lazy {
        pythonInstance.getModule("tire_processing")
    }

    // ===== STATE FLOWS =====

    // Mode operasi saat ini
    private val _appMode = MutableStateFlow(AppMode.TERMINAL)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    // Status koneksi Bluetooth
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Terminal log text
    private val _terminalText = MutableStateFlow("")
    val terminalText: StateFlow<String> = _terminalText.asStateFlow()

    // State CekBan
    private val _cekBanState = MutableStateFlow(CekBanState.IDLE)
    val cekBanState: StateFlow<CekBanState> = _cekBanState.asStateFlow()

    // Status message untuk user
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Buffer untuk 4 alur
    private val scanBuffer = mutableListOf<String>()

    // Counter data yang masuk
    private val _dataCount = MutableStateFlow(0)
    val dataCount: StateFlow<Int> = _dataCount.asStateFlow()

    // ===== CONTEXT CEK BAN =====
    private var currentPengecekanId: Long? = null
    private var currentBusId: Long? = null
    private var currentPosisi: PosisiBan? = null

    // Hasil scan per posisi (sementara, sebelum disimpan)
    private val _scanResults = MutableStateFlow<Map<PosisiBan, TireScanResult>>(emptyMap())
    val scanResults: StateFlow<Map<PosisiBan, TireScanResult>> = _scanResults.asStateFlow()

    init {
        setupBluetoothCallbacks()
    }

    private fun setupBluetoothCallbacks() {
        // Callback saat data diterima
        bluetoothHelper.onDataReceived = { rawData ->
            addToTerminal(rawData)

            // Jika dalam mode CEK_BAN dan state SCANNING
            if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
                scanBuffer.add(rawData)

                _dataCount.value = scanBuffer.size

                // Auto-stop jika data cukup (misal 500-1000 baris untuk 1 posisi)
                if (scanBuffer.size >= 500) {
                    Log.d(TAG, "Buffer cukup (${scanBuffer.size} baris), auto-processing...")
                    processCurrentScan()
                }
            }
        }

        // Callback status koneksi
        bluetoothHelper.onStatusChange = { status ->
            val connected = status.contains("Connected", ignoreCase = true)
            _isConnected.value = connected
            addToTerminal("SYSTEM: $status")

            // Jika mode CekBan dan berhasil connect
            if (_appMode.value == AppMode.CEK_BAN && connected) {
                _statusMessage.value = "Bluetooth terhubung! Silakan pilih posisi ban."
            }
        }
    }

    // ===== TERMINAL MODE FUNCTIONS =====

    /**
     * Switch ke mode Terminal (free mode)
     */
    fun switchToTerminalMode() {
        if (_appMode.value != AppMode.TERMINAL) {
            addToTerminal("\n=== MODE TERMINAL (FREE MODE) ===")
            _appMode.value = AppMode.TERMINAL
            resetCekBanContext()
            _statusMessage.value = "Mode Terminal aktif. Data tidak akan disimpan ke database."
        }
    }

    /**
     * Connect manual (untuk Terminal mode)
     */
    fun connectBluetooth() {
        addToTerminal("SYSTEM: Menghubungkan Bluetooth...")
        bluetoothHelper.connect()
    }

    fun disconnectBluetooth() {
        bluetoothHelper.disconnect()
        _isConnected.value = false

        // Jika sedang dalam proses CekBan, reset
        if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
            _cekBanState.value = CekBanState.ERROR
            _statusMessage.value = "Koneksi terputus saat scanning"
        }
    }

    fun sendCommand(command: String) {
        bluetoothHelper.send(command)
        addToTerminal("SENT: $command")
    }

    // ===== CEK BAN MODE FUNCTIONS =====

    /**
     * Masuk ke mode CekBan dengan konteks bus & pengecekan
     */
    fun enterCekBanMode(busId: Long, pengecekanId: Long) {
        _appMode.value = AppMode.CEK_BAN
        currentBusId = busId
        currentPengecekanId = pengecekanId
        _cekBanState.value = CekBanState.IDLE
        _scanResults.value = emptyMap()

        addToTerminal("\n=== MODE CEK BAN ===")
        addToTerminal("Bus ID: $busId")
        addToTerminal("Pengecekan ID: $pengecekanId")

        // Auto-connect jika belum connect
        if (!_isConnected.value) {
            _statusMessage.value = "Menghubungkan Bluetooth..."
            connectBluetooth()
        } else {
            _statusMessage.value = "Silakan pilih posisi ban untuk scan"
        }
    }

    /**
     * User pilih posisi ban yang akan di-scan
     */
    fun selectPositionToScan(posisi: PosisiBan) {
        if (_appMode.value != AppMode.CEK_BAN) {
            _statusMessage.value = "Error: Tidak dalam mode CekBan"
            return
        }

        if (!_isConnected.value) {
            _statusMessage.value = "Error: Bluetooth belum terhubung"
            return
        }

        // Cek apakah posisi ini sudah di-scan
        if (_scanResults.value.containsKey(posisi)) {
            _statusMessage.value = "Posisi ${posisi.label} sudah di-scan. Scan ulang?"
            // Bisa tambahkan konfirmasi dialog di UI
        }

        currentPosisi = posisi
        _cekBanState.value = CekBanState.WAITING_SCAN

        _statusMessage.value = "Siap scan posisi ${posisi.label}. Tekan 'Mulai Scan'."
        addToTerminal("\n--- POSISI TERPILIH: ${posisi.label} ---")
    }

    /**
     * Mulai scan posisi yang sudah dipilih
     */
    fun startScan() {
        if (currentPosisi == null) {
            _statusMessage.value = "Error: Pilih posisi ban dulu"
            return
        }

        if (_cekBanState.value != CekBanState.WAITING_SCAN) {
            _statusMessage.value = "Error: State tidak valid untuk mulai scan"
            return
        }

        // Reset buffer
        scanBuffer.clear()
        _dataCount.value = 0

        // Mulai scan
        _cekBanState.value = CekBanState.SCANNING
        _statusMessage.value = "Scanning posisi ${currentPosisi!!.label}... Dekatkan sensor ke ban"

        // Kirim START command ke STM32
        sendCommand("START")

        addToTerminal(">>> MULAI SCAN POSISI ${currentPosisi!!.label}")
    }

    /**
     * Stop scan manual (atau auto-triggered setelah cukup data)
     */
    fun stopScan() {
        if (_cekBanState.value != CekBanState.SCANNING) {
            return
        }

        sendCommand("STOP")
        _statusMessage.value = "Menghentikan scan..."
        processCurrentScan()
    }

    /**
     * Process data yang sudah di-scan untuk posisi saat ini
     */
    fun processCurrentScan() {
        if (scanBuffer.isEmpty()) {
            _statusMessage.value = "Tidak ada data untuk diproses"
            _cekBanState.value = CekBanState.ERROR
            return
        }

        if (currentPosisi == null) {
            _statusMessage.value = "Error: Posisi tidak valid"
            _cekBanState.value = CekBanState.ERROR
            return
        }

        _cekBanState.value = CekBanState.PROCESSING
        _statusMessage.value = "Memproses ${scanBuffer.size} data untuk ${currentPosisi!!.label}..."

        viewModelScope.launch(Dispatchers.Default) {
            try {
                addToTerminal("\n=== PROCESSING ${currentPosisi!!.label} ===")
                addToTerminal("Total lines: ${scanBuffer.size}")

                // ✅ Convert to ArrayList explicitly
                val javaList = java.util.ArrayList(scanBuffer.toList())

                addToTerminal("Calling Python with ${javaList.size} lines...")

                // ✅ Call Python
                val resultJson = processingModule.callAttr(
                    "process_single_sensor",
                    javaList  // Pass as Java ArrayList
                ).toString()

                addToTerminal("Python response received")
                addToTerminal(resultJson.take(200) + "...")  // Log first 200 chars

                val jsonObj = JSONObject(resultJson)
                val success = jsonObj.getBoolean("success")
                val message = jsonObj.getString("message")

                addToTerminal("Status: $success - $message")

                if (success) {
                    val data = jsonObj.getJSONObject("result")
                    val result = TireScanResult(
                        posisi = currentPosisi!!,
                        adcMean = data.getDouble("adc_mean").toFloat(),
                        adcStd = data.getDouble("adc_std").toFloat(),
                        voltageMv = data.getDouble("voltage_mV").toFloat(),
                        isWorn = data.getBoolean("is_worn"),
                        dataCount = data.getInt("pixel_count"),

                        alur1 = data.getDouble("alur1").toFloat(),
                        alur2 = data.getDouble("alur2").toFloat(),
                        alur3 = data.getDouble("alur3").toFloat(),
                        alur4 = data.getDouble("alur4").toFloat()
                    )

                    val status = if (result.isWorn) "AUS ❌" else "OK ✅"
                    val minGroove = result.minGroove
                    addToTerminal("${currentPosisi!!.label}: Min: ${minGroove} mm - $status")

                    withContext(Dispatchers.Main) {
                        // Simpan hasil ke map
                        val updatedResults = _scanResults.value.toMutableMap()
                        updatedResults[currentPosisi!!] = result
                        _scanResults.value = updatedResults

                        _cekBanState.value = CekBanState.RESULT_READY
                        _statusMessage.value = "${currentPosisi!!.label}: Min ${minGroove} mm $status"
                    }

                } else {
                    // Python returned success=false
                    addToTerminal("ERROR: $message")

                    withContext(Dispatchers.Main) {
                        _cekBanState.value = CekBanState.ERROR
                        _statusMessage.value = "Error: $message"
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}", e)
                addToTerminal("EXCEPTION: ${e.javaClass.simpleName}")
                addToTerminal("Message: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    _cekBanState.value = CekBanState.ERROR
                    _statusMessage.value = "Gagal memproses: ${e.message}"
                }
            }
        }
    }

    /**
     * Konfirmasi hasil scan dan lanjut ke posisi berikutnya
     */
    fun confirmScanResult() {
        if (_cekBanState.value != CekBanState.RESULT_READY) {
            return
        }

        val posLabel = currentPosisi?.label ?: "Unknown"
        addToTerminal("✓ Hasil ${posLabel} dikonfirmasi\n")

        // Reset untuk scan berikutnya
        currentPosisi = null
        scanBuffer.clear()
        _dataCount.value = 0

        // ✅ Kembali ke IDLE agar user bisa pilih posisi lain
        _cekBanState.value = CekBanState.IDLE
        _statusMessage.value = "Pilih posisi ban berikutnya atau simpan semua"
    }

    /**
     * Simpan semua hasil scan ke database
     */

    fun saveAllResults() {
        val results = _scanResults.value
        if (results.isEmpty()) {
            _statusMessage.value = "Belum ada hasil untuk disimpan"
            return
        }

        if (currentPengecekanId == null) {
            _statusMessage.value = "Error: ID pengecekan tidak valid"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                addToTerminal("\n=== SAVING TO DATABASE ===")
                addToTerminal("Pengecekan ID: $currentPengecekanId")
                addToTerminal("Results count: ${results.size}")

                val statusMap = mutableMapOf<PosisiBan, Boolean?>()
                for((posisi, result) in results) {
                    val statusBan: Boolean? = result.isWorn
                    statusMap[posisi] = statusBan

                    // Update DetailBan
                    var detailBan = detailBanDao.getDetailByPosisi(
                        currentPengecekanId!!,
                        posisi.name // "DKA", "DKI", dll.
                    )

                    if (detailBan == null) {
                        detailBan = DetailBan(
                            pengecekanId = currentPengecekanId!!,
                            posisiBan = posisi.name,
                            status = statusBan
                        )
                    } else {
                        detailBan = detailBan.copy(status = statusBan)
                    }

                    val detailBanId = detailBanDao.insertDetailBan(detailBan)
                    addToTerminal("Saved DetailBan for ${posisi.name} (ID: $detailBanId)")

                    var pengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detailBanId)

                    if (pengukuran == null) {
                        // Belum ada, buat baru
                        pengukuran = com.example.tetires.data.local.entity.PengukuranAlur(
                            detailBanId = detailBanId,
                            alur1 = result.alur1,
                            alur2 = result.alur2,
                            alur3 = result.alur3,
                            alur4 = result.alur4
                        )
                    } else {
                        pengukuran = pengukuran.copy(
                            alur1 = result.alur1,
                            alur2 = result.alur2,
                            alur3 = result.alur3,
                            alur4 = result.alur4
                        )
                    }

                    pengukuranAlurDao.insertPengukuran(pengukuran)
                    addToTerminal("Saved PengukuranAlur for ${posisi.name}")
                }

                // Ambil pengecekan yang ada
                val pengecekan = pengecekanDao.getPengecekanById(currentPengecekanId!!)
                if (pengecekan != null) {

                    val updatedPengecekan = pengecekan.copy(
                        statusDka = statusMap[PosisiBan.DKA],
                        statusDki = statusMap[PosisiBan.DKI],
                        statusBka = statusMap[PosisiBan.BKA],
                        statusBki = statusMap[PosisiBan.BKI]
                    )
                    pengecekanDao.updatePengecekan(updatedPengecekan)
                    addToTerminal("Updated Pengecekan summary")
                }

                withContext(Dispatchers.Main) {
                    _cekBanState.value = CekBanState.SAVED
                    _statusMessage.value = "Semua data berhasil disimpan!"
                    addToTerminal("=== DATABASE SAVE COMPLETE ===")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving: ${e.message}", e)
                addToTerminal("ERROR DB: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Gagal menyimpan: ${e.message}"
                    _cekBanState.value = CekBanState.ERROR
                }
            }
        }
    }



    /**
     * Reset context CekBan (keluar dari mode CekBan)
     */
    fun resetCekBanContext() {
        currentPengecekanId = null
        currentBusId = null
        currentPosisi = null
        scanBuffer.clear()
        _dataCount.value = 0
        _scanResults.value = emptyMap()
        _cekBanState.value = CekBanState.IDLE
    }

    // ===== UTILITY FUNCTIONS =====

    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun addToTerminal(text: String) {
        val timestamp = getCurrentTimestamp()
        _terminalText.value += "\n[$timestamp] $text"
    }

    fun clearTerminal() {
        _terminalText.value = ""
    }


    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothHelper.disconnect()
    }
}



/**
 * Data class untuk hasil scan satu posisi ban
 */
data class TireScanResult(
    val posisi: PosisiBan,
    val adcMean: Float,
    val adcStd: Float,
    val voltageMv: Float,
    val isWorn: Boolean,
    val dataCount: Int,

    val alur1: Float,
    val alur2: Float,
    val alur3: Float,
    val alur4: Float,
) {
    val minGroove: Float
        get() = listOf(alur1, alur2, alur3, alur4).minOrNull() ?: 0f

    val groovesFormatted: String
        get() = "${"%.1f".format(alur1)} | ${"%.1f".format(alur2)} | ${"%.1f".format(alur3)} | ${"%.1f".format(alur4)}"

    // Konversi ke FloatArray untuk penyimpanan
    fun toAlurArray(): FloatArray = floatArrayOf(alur1, alur2, alur3, alur4)
}