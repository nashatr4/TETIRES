package com.example.tetires.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.PengukuranAlur
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

class BluetoothSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BluetoothSharedVM"

    // Database + DAO
    private val database = AppDatabase.getInstance(application)
    private val busDao = database.busDao()
    private val pengecekanDao = database.pengecekanDao()
    private val detailBanDao = database.detailBanDao()
    private val pengukuranAlurDao = database.pengukuranAlurDao()

    private val repository = com.example.tetires.data.repository.TetiresRepository(
        busDao, pengecekanDao, detailBanDao, pengukuranAlurDao
    )

    // Device manager (Bluetooth/USB/Serial)
    private val deviceManager = DeviceConnectionManager(application)

    // Python
    private val pythonInstance: Python = Python.getInstance()
    private val processingModule by lazy { pythonInstance.getModule("tire_depth") }

    // STATE FLOWS
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

    private val scanBuffer = mutableListOf<String>()
    private val _dataCount = MutableStateFlow(0)
    val dataCount: StateFlow<Int> = _dataCount.asStateFlow()

    // CONTEXT CEK BAN
    // ✅ UBAH: Pakai idCek yang konsisten
    private var currentIdCek: Long? = null
    private var currentBusId: Long? = null
    private var currentPosisi: PosisiBan? = null

    // ✅ TAMBAHKAN: Getter untuk akses dari luar
    val savedIdCek: Long?
        get() = currentIdCek

    private val _scanResults = MutableStateFlow<Map<PosisiBan, TireScanResult>>(emptyMap())
    val scanResults: StateFlow<Map<PosisiBan, TireScanResult>> = _scanResults.asStateFlow()

    init {
        setupDeviceManagerCallbacks()
    }

    private fun setupDeviceManagerCallbacks() {
        deviceManager.onDataReceived = { rawData ->
            addToTerminal(rawData)

            if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
                scanBuffer.add(rawData)
                _dataCount.value = scanBuffer.size

                if (scanBuffer.size >= 1110) {
                    Log.d(TAG, "Buffer penuh (${scanBuffer.size}), proses otomatis.")
                    processCurrentScan()
                }
            }
        }

        deviceManager.onStatusChange = { status ->
            _statusMessage.value = status
            addToTerminal("SYSTEM: $status")
        }

        deviceManager.onDebugLog = { debugMsg ->
            if (_appMode.value == AppMode.TERMINAL) addToTerminal(debugMsg)
        }

        viewModelScope.launch {
            deviceManager.isConnected.collect { connected ->
                _isConnected.value = connected
                if (_appMode.value == AppMode.CEK_BAN && connected && _cekBanState.value == CekBanState.IDLE) {
                    _statusMessage.value = "Device terhubung! Pilih posisi ban untuk scan."
                }
            }
        }

        viewModelScope.launch {
            deviceManager.deviceInfo.collect { info -> _deviceInfo.value = info }
        }

        viewModelScope.launch {
            deviceManager.activeDevice.collect { deviceType ->
                _activeDeviceType.value = deviceType
                addToTerminal("Active Device: ${deviceType.name}")
            }
        }
    }

    // TERMINAL
    fun switchToTerminalMode() {
        if (_appMode.value != AppMode.TERMINAL) {
            addToTerminal("\n=== MODE TERMINAL ===")
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
        if (_appMode.value == AppMode.CEK_BAN && _cekBanState.value == CekBanState.SCANNING) {
            _cekBanState.value = CekBanState.ERROR
            _statusMessage.value = "Koneksi terputus saat scanning"
        }
    }

    fun sendCommand(command: String) {
        deviceManager.sendCommand(command)
        addToTerminal("SENT: $command")
    }

    // CEK BAN
    // ✅ UBAH: Parameter pakai idCek
    fun enterCekBanMode(busId: Long, idCek: Long) {
        _appMode.value = AppMode.CEK_BAN
        currentBusId = busId
        _cekBanState.value = CekBanState.IDLE
        _scanResults.value = emptyMap()

        addToTerminal("\n=== MODE CEK BAN ===")
        addToTerminal("Bus ID: $busId")
        addToTerminal("ID Cek: $idCek")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idCekAktif: Long

                if (idCek == 0L) {
                    // Buat pengecekan baru
                    val pengecekanBaru = repository.startOrGetOpenCheck(busId)
                    idCekAktif = pengecekanBaru.idPengecekan
                    addToTerminal("Pengecekan baru dibuat. ID: $idCekAktif")
                } else {
                    // Edit pengecekan existing
                    addToTerminal("Mengedit Pengecekan ID: $idCek")
                    idCekAktif = idCek
                }

                // ✅ Simpan idCek aktif
                currentIdCek = idCekAktif

                // Koneksi device
                if (!_isConnected.value) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Menghubungkan device..."
                    }
                    deviceManager.autoDetectDevices()
                } else {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Pilih posisi ban untuk scan"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal startOrGetOpenCheck: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: Gagal memulai pengecekan"
                    _cekBanState.value = CekBanState.ERROR
                }
            }
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalData = scanBuffer.size
                Log.d(TAG, "Proses data: $totalData lines")

                val javaList = java.util.ArrayList(scanBuffer.toList())
                val pyList = com.chaquo.python.PyObject.fromJava(javaList)

                addToTerminal("Calling Python with ${javaList.size} lines...")
                val resultJson = processingModule.callAttr("process_single_sensor", pyList).toString()
                addToTerminal("Python response received")
                addToTerminal(resultJson.take(300) + if (resultJson.length > 300) "..." else "")

                val json = JSONObject(resultJson)
                val success = json.optBoolean("success", false)

                if (!success) {
                    val message = json.optString("message", "Processing failed")
                    withContext(Dispatchers.Main) {
                        _cekBanState.value = CekBanState.ERROR
                        _statusMessage.value = "Error: $message"
                    }
                    return@launch
                }

                val data = json.getJSONObject("result")

                val alur1 = data.optDouble("alur1", 0.0).toFloat()
                val alur2 = data.optDouble("alur2", 0.0).toFloat()
                val alur3 = data.optDouble("alur3", 0.0).toFloat()
                val alur4 = data.optDouble("alur4", 0.0).toFloat()

                val adcMean = data.optDouble("adc_mean", 0.0).toFloat()
                val adcStd = data.optDouble("adc_std", 0.0).toFloat()
                val voltageMv = data.optDouble("voltage_mV", 0.0).toFloat()
                val isWorn = data.optBoolean("is_worn", false)
                val pixelCount = data.optInt("pixel_count", totalData)

                val result = TireScanResult(
                    posisi = currentPosisi!!,
                    adcMean = adcMean,
                    adcStd = adcStd,
                    voltageMv = voltageMv,
                    isWorn = isWorn,
                    dataCount = pixelCount,
                    alur1 = alur1,
                    alur2 = alur2,
                    alur3 = alur3,
                    alur4 = alur4
                )

                val updated = _scanResults.value.toMutableMap()
                updated[currentPosisi!!] = result
                _scanResults.value = updated

                withContext(Dispatchers.Main) {
                    addToTerminal(
                        """
                        === HASIL ${currentPosisi?.label} ===
                        4 Alur: ${result.groovesFormatted}
                        Min: ${result.minGrooveLabel} = ${"%.1f".format(result.minGroove)} mm
                        Status: ${if (result.isWorn) "AUS ❌" else "AMAN ✅"}
                        ===========================
                        """.trimIndent()
                    )
                    _cekBanState.value = CekBanState.RESULT_READY
                    _statusMessage.value = "${currentPosisi!!.label}: ${result.minGrooveLabel} = ${"%.1f".format(result.minGroove)}mm → ${if (result.isWorn) "AUS ❌" else "AMAN ✅"}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error proses: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _cekBanState.value = CekBanState.ERROR
                    _statusMessage.value = "Error: ${e.message}"
                    addToTerminal("ERROR: ${e.message}")
                }
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

    // ✅ FIXED: Simpan dengan idCek yang benar
    fun saveAllResults() {
        val results = _scanResults.value
        if (results.isEmpty()) {
            _statusMessage.value = "Belum ada hasil untuk disimpan"
            return
        }
        if (currentIdCek == null) {
            _statusMessage.value = "Error: ID pengecekan tidak valid"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                addToTerminal("\n=== SAVING TO DATABASE ===")
                addToTerminal("ID Cek: $currentIdCek")

                val statusMap = mutableMapOf<PosisiBan, Boolean?>()

                for ((posisi, result) in results) {
                    val statusBan: Boolean = result.isWorn

                    // ✅ Gunakan currentIdCek
                    var detailBan = detailBanDao.getDetailByPosisi(currentIdCek!!, posisi.name)
                    if (detailBan == null) {
                        detailBan = DetailBan(
                            pengecekanId = currentIdCek!!,
                            posisiBan = posisi.name,
                            status = statusBan
                        )
                        val detailId = detailBanDao.insertDetailBan(detailBan)
                        addToTerminal("Saved DetailBan for ${posisi.name} (ID: $detailId)")

                        val pengukuran = PengukuranAlur(
                            detailBanId = detailId,
                            alur1 = result.alur1,
                            alur2 = result.alur2,
                            alur3 = result.alur3,
                            alur4 = result.alur4
                        )
                        pengukuranAlurDao.insertPengukuran(pengukuran)
                        addToTerminal("Saved PengukuranAlur: ${result.groovesFormatted}")
                    } else {
                        val updatedDetail = detailBan.copy(status = statusBan)
                        detailBanDao.updateDetailBan(updatedDetail)
                        addToTerminal("Updated DetailBan for ${posisi.name}")

                        val existingPeng = pengukuranAlurDao.getPengukuranByDetailBanId(detailBan.idDetail)
                        if (existingPeng == null) {
                            val pengukuran = PengukuranAlur(
                                detailBanId = detailBan.idDetail,
                                alur1 = result.alur1,
                                alur2 = result.alur2,
                                alur3 = result.alur3,
                                alur4 = result.alur4
                            )
                            pengukuranAlurDao.insertPengukuran(pengukuran)
                        } else {
                            val updatedPeng = existingPeng.copy(
                                alur1 = result.alur1,
                                alur2 = result.alur2,
                                alur3 = result.alur3,
                                alur4 = result.alur4
                            )
                            pengukuranAlurDao.updatePengukuran(updatedPeng)
                        }
                        addToTerminal("Updated PengukuranAlur: ${result.groovesFormatted}")
                    }

                    statusMap[posisi] = statusBan
                }

                // ✅ Update summary pengecekan
                val pengecekan = pengecekanDao.getPengecekanById(currentIdCek!!)
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
                    _statusMessage.value = "Semua data berhasil disimpan! ID: $currentIdCek"
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
        currentIdCek = null
        currentBusId = null
        currentPosisi = null
        scanBuffer.clear()
        _dataCount.value = 0
        _scanResults.value = emptyMap()
        _cekBanState.value = CekBanState.IDLE
    }

    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun addToTerminal(text: String) {
        val timestamp = getCurrentTimestamp()
        _terminalText.value += "\n[$timestamp] $text"
    }

    fun clearTerminal() { _terminalText.value = "" }
    fun clearStatusMessage() { _statusMessage.value = null }

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

    val minGrooveLabel: String
        get() {
            val alurMap = listOf(
                "Alur 1" to alur1,
                "Alur 2" to alur2,
                "Alur 3" to alur3,
                "Alur 4" to alur4
            )
            val minEntry = alurMap.minByOrNull { it.second }
            return minEntry?.first ?: "?"
        }

    val groovesFormatted: String
        get() = "${"%.1f".format(alur1)} | ${"%.1f".format(alur2)} | ${"%.1f".format(alur3)} | ${"%.1f".format(alur4)}"

    val minGrooveFormatted: String
        get() = "${"%.1f".format(minGroove)} mm"

    fun toAlurArray(): FloatArray = floatArrayOf(alur1, alur2, alur3, alur4)

    val statusText: String
        get() = if (isWorn) "AUS (< 1.6mm)" else "AMAN (≥ 1.6mm)"
}