package com.example.tetires.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.model.PosisiBan
import com.example.tetires.util.DeviceConnectionManager
import com.example.tetires.util.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class CekBanState {
    IDLE, WAITING_SCAN, SCANNING, PROCESSING, RESULT_READY, SAVED, ERROR
}

enum class AppMode {
    TERMINAL, CEK_BAN
}

/**
 * 🔥 UPDATED: Shared ViewModel dengan DeviceConnectionManager
 */
class BluetoothSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BluetoothSharedVM"

    // Database
    private val database = AppDatabase.getInstance(application)
    private val busDao = database.busDao()
    private val pengecekanDao = database.pengecekanDao()
    private val detailBanDao = database.detailBanDao()
    private val pengukuranAlurDao = database.pengukuranAlurDao()

    private val repository = com.example.tetires.data.repository.TetiresRepository(
        busDao, pengecekanDao, detailBanDao
    )

    // 🔥 NEW: Device Connection Manager
    private val deviceManager = DeviceConnectionManager(application)

    // Python
    private val pythonInstance: Python = Python.getInstance()
    private val processingModule by lazy { pythonInstance.getModule("tire_processing") }

    // ===== STATE FLOWS =====
    private val _appMode = MutableStateFlow(AppMode.TERMINAL)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _deviceInfo = MutableStateFlow("No device")
    val deviceInfo: StateFlow<String> = _deviceInfo.asStateFlow()

    private val _activeDeviceType = MutableStateFlow(DeviceType.NONE)
    val activeDeviceType: StateFlow<DeviceType> = _activeDeviceType.asStateFlow()

    private val _terminalText = MutableStateFlow("")
    val terminalText: StateFlow<String> = _terminalText.asStateFlow()

    private val _cekBanState = MutableStateFlow(CekBanState.IDLE)
    val cekBanState: StateFlow<CekBanState> = _cekBanState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Buffer untuk 4 alur
    private val scanBuffer = mutableListOf<String>()

    private val _dataCount = MutableStateFlow(0)
    val dataCount: StateFlow<Int> = _dataCount.asStateFlow()

    // ===== CONTEXT CEK BAN =====
    private var currentPengecekanId: Long? = null
    private var currentBusId: Long? = null
    private var currentPosisi: PosisiBan? = null

    private val _scanResults = MutableStateFlow<Map<PosisiBan, TireScanResult>>(emptyMap())
    val scanResults: StateFlow<Map<PosisiBan, TireScanResult>> = _scanResults.asStateFlow()

    init {
        setupDeviceManagerCallbacks()

        // 🔥 Auto-detect devices saat init
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            deviceManager.autoDetectDevices()
        }
    }

    /**
     * 🔥 Setup callbacks dari DeviceManager
     */
    private fun setupDeviceManagerCallbacks() {
        // Data received callback
        deviceManager.onDataReceived = { rawData ->
            addToTerminal(rawData)

            // Jika dalam mode CEK_BAN dan state SCANNING
            if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
                scanBuffer.add(rawData)

                _dataCount.value = scanBuffer.size

                if (scanBuffer.size == 1110) {
                    Log.d(TAG, "Buffer penuh (1110 data), proses otomatis!")

                    processCurrentScan()
                }
            }
        }

        // Status change callback
        deviceManager.onStatusChange = { status ->
            _statusMessage.value = status
            addToTerminal("SYSTEM: $status")
        }

        // Debug log callback (untuk Terminal)
        deviceManager.onDebugLog = { debugMsg ->
            if (_appMode.value == AppMode.TERMINAL) {
                addToTerminal(debugMsg)
            }
        }

        // Collect connection state
        viewModelScope.launch {
            deviceManager.isConnected.collect { connected ->
                _isConnected.value = connected

                if (_appMode.value == AppMode.CEK_BAN && connected) {
                    if (_cekBanState.value == CekBanState.IDLE) {
                        _statusMessage.value = "Device terhubung! Pilih posisi ban untuk scan."
                    }
                }
            }
        }

        // Collect device info
        viewModelScope.launch {
            deviceManager.deviceInfo.collect { info ->
                _deviceInfo.value = info
            }
        }

        // Collect active device type
        viewModelScope.launch {
            deviceManager.activeDevice.collect { deviceType ->
                _activeDeviceType.value = deviceType
                addToTerminal("Active Device: ${deviceType.name}")
            }
        }
    }

    // ===== TERMINAL MODE FUNCTIONS =====

    fun switchToTerminalMode() {
        if (_appMode.value != AppMode.TERMINAL) {
            addToTerminal("\n=== MODE TERMINAL (FREE MODE) ===")
            _appMode.value = AppMode.TERMINAL
            resetCekBanContext()
            _statusMessage.value = "Mode Terminal aktif"
        }
    }

    fun connectBluetooth() {
        addToTerminal("SYSTEM: Manual connect triggered...")
        deviceManager.manualConnect()
    }

    fun disconnectBluetooth() {
        deviceManager.disconnect()

        // Jika sedang scanning, set error
        if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
            _cekBanState.value = CekBanState.ERROR
            _statusMessage.value = "Koneksi terputus saat scanning"
        }
    }

    fun sendCommand(command: String) {
        deviceManager.sendCommand(command)
        addToTerminal("SENT: $command")
    }

    // ===== CEK BAN MODE FUNCTIONS =====

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
            _statusMessage.value = "Menghubungkan device..."
            viewModelScope.launch {
                deviceManager.autoDetectDevices()
            }
        } else {
            _statusMessage.value = "Pilih posisi ban untuk scan"
        }
    }

    fun selectPositionToScan(posisi: PosisiBan) {
        if (_appMode.value != AppMode.CEK_BAN) {
            _statusMessage.value = "Error: Tidak dalam mode CekBan"
            return
        }

        if (!_isConnected.value) {
            _statusMessage.value = "Error: Device belum terhubung"
            return
        }

        if (_scanResults.value.containsKey(posisi)) {
            _statusMessage.value = "Posisi ${posisi.label} sudah di-scan. Scan ulang?"
        }

        currentPosisi = posisi
        _cekBanState.value = CekBanState.WAITING_SCAN
        _statusMessage.value = "Siap scan ${posisi.label}. Tekan 'Mulai Scan'."
        addToTerminal("\n--- POSISI: ${posisi.label} ---")
    }

    fun startScan() {
        if (currentPosisi == null) {
            _statusMessage.value = "Error: Pilih posisi ban dulu"
            return
        }

        if (_cekBanState.value != CekBanState.WAITING_SCAN) {
            _statusMessage.value = "Error: State tidak valid"
            return
        }

        scanBuffer.clear()
        _dataCount.value = 0
        _cekBanState.value = CekBanState.SCANNING
        _statusMessage.value = "Scanning ${currentPosisi!!.label}..."

        sendCommand("START")
        addToTerminal(">>> MULAI SCAN ${currentPosisi!!.label}")
    }

    fun stopScan() {
        if (_cekBanState.value != CekBanState.SCANNING) return
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
                val totalData = scanBuffer.size
                Log.d(TAG, "Proses data: $totalData line")
                _cekBanState.value = CekBanState.PROCESSING

                if (totalData == 0) {
                    Log.e(TAG, "Gagal: belum ada data")
                    return@launch
                }

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
                val javaList = java.util.ArrayList(scanBuffer.toList())
                val pyList = com.chaquo.python.PyObject.fromJava(javaList)

                Log.d(TAG, "Panggil Python dengan ${javaList.size} item...")
                val resultJson = processingModule.callAttr("process_single_sensor", pyList).toString()
                Log.d(TAG, "Hasil: $resultJson")

                val json = JSONObject(resultJson)

                        _cekBanState.value = CekBanState.RESULT_READY
                        _statusMessage.value = "${currentPosisi!!.label}: Min ${minGroove} mm $status"
                    }

// Cek validasi
                val thicknessMm = if (rawThickness.isFinite() && rawThickness > 0) {
                    rawThickness.toFloat()
                } else {
                    // Python returned success=false
                    addToTerminal("ERROR: $message")

                    withContext(Dispatchers.Main) {
                        _cekBanState.value = CekBanState.ERROR
                        _statusMessage.value = "Error: $message"
                    }
                }

// Buat objek hasil, tapi tanpa nilai 0 palsu
                val result = TireScanResult(
                    posisi = currentPosisi ?: PosisiBan.DKA,
                    adcMean = json.optDouble("adc_mean", Double.NaN).toFloat(),
                    adcStd = json.optDouble("adc_std", Double.NaN).toFloat(),
                    voltageMv = json.optDouble("voltage_mv", Double.NaN).toFloat(),
                    thicknessMm = thicknessMm,
                    isWorn = json.optBoolean("is_worn", false),
                    dataCount = totalData
                )


                val updatedResults = _scanResults.value.toMutableMap()
                updatedResults[currentPosisi!!] = result
                _scanResults.value = updatedResults

                withContext(Dispatchers.Main) {
                    addToTerminal(
                        """
                        === HASIL ${currentPosisi?.label} ===
                        Mean ADC: ${result.adcMean}
                        Tegangan: ${result.voltageMv} mV
                        Ketebalan: ${result.thicknessMm} mm
                        Status: ${if (result.isWorn) "AUS" else "OK"}
                        ===========================
                        """.trimIndent()
                    )
                    _cekBanState.value = CekBanState.RESULT_READY
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error proses: ${e.message}", e)
            }
        }
    }

    fun confirmScanResult() {
        if (_cekBanState.value != CekBanState.RESULT_READY) return

        val posLabel = currentPosisi?.label ?: "Unknown"
        addToTerminal("✓ Hasil $posLabel dikonfirmasi\n")

        currentPosisi = null
        scanBuffer.clear()
        _dataCount.value = 0
        _cekBanState.value = CekBanState.IDLE
        _statusMessage.value = "Pilih posisi berikutnya atau simpan semua"
    }

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
                val pengecekan = pengecekanDao.getPengecekanById(currentPengecekanId!!)
                if (pengecekan == null) {
                    addToTerminal("ERROR: Pengecekan tidak ditemukan")
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error: Data tidak ditemukan"
                        _cekBanState.value = CekBanState.ERROR
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

                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Gagal menyimpan: ${e.message}"
                    _cekBanState.value = CekBanState.ERROR
                }
            }
        }
    }

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
        deviceManager.cleanup()
    }
}

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