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
import kotlinx.coroutines.delay

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
    private val busDao = database.busDao()               // <--- tambahin ini
    private val pengecekanDao = database.pengecekanDao()
    private val detailBanDao = database.detailBanDao()

    // Repository (pastikan constructor TetiresRepository menerima BusDao, PengecekanDao, DetailBanDao)
    private val repository = com.example.tetires.data.repository.TetiresRepository(
        busDao,
        pengecekanDao,
        detailBanDao
    )

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

    // Data buffer untuk scan satu posisi
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

                if (scanBuffer.size == 1110) {
                    Log.d(TAG, "Buffer penuh (1110 data), langsung proses!")
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Gak perlu delay, langsung ambil snapshot buffer
                val totalData = scanBuffer.size
                Log.d("TETIRES", "Mulai proses data: $totalData line")
                _cekBanState.value = CekBanState.PROCESSING

                if (totalData == 0) {
                    Log.e("TETIRES", "Gagal: belum ada data yang diterima dari Bluetooth.")
                    return@launch
                }

                // 🔹 3. Konversi ke ArrayList<String> dan kirim ke Python
                val javaList = java.util.ArrayList(scanBuffer.toList())

                val py = Python.getInstance()
                val processingModule = py.getModule("tire_processing")

                // Gunakan PyObject.fromJava biar data benar dikirim sebagai list
                val pyList = com.chaquo.python.PyObject.fromJava(javaList)

                Log.d("TETIRES", "Memanggil Python dengan ${javaList.size} item...")
                val resultJson = processingModule.callAttr("process_single_sensor", pyList).toString()
                Log.d("TETIRES", "Hasil Python: $resultJson")

                // 🔹 Parse hasil JSON dari Python
                val json = JSONObject(resultJson)

                val result = TireScanResult(
                    posisi = currentPosisi ?: PosisiBan.DKA, // fallback biar gak null
                    adcMean = json.optDouble("adc_mean", 0.0).toFloat(),
                    adcStd = json.optDouble("adc_std", 0.0).toFloat(),
                    voltageMv = json.optDouble("voltage_mv", 0.0).toFloat(),
                    thicknessMm = json.optDouble("thickness_mm", 0.0).toFloat(),
                    isWorn = json.optBoolean("is_worn", false),
                    dataCount = totalData
                )

                // 🔹 Update hasil ke state flow
                                val updatedResults = _scanResults.value.toMutableMap()
                                updatedResults[currentPosisi!!] = result
                                _scanResults.value = updatedResults

                // 🔹 Tampilkan di terminal
                                withContext(Dispatchers.Main) {
                                    addToTerminal(
                                        """
                        === HASIL ${currentPosisi?.label} ===
                        Mean ADC: ${result.adcMean}
                        STD ADC: ${result.adcStd}
                        Tegangan: ${result.voltageMv} mV
                        Ketebalan: ${result.thicknessMm} mm
                        Status: ${if (result.isWorn) "AUS" else "OK"}
                        ===========================
                        """.trimIndent()
                                    )
                    _cekBanState.value = CekBanState.RESULT_READY
                }
            } catch (e: Exception) {
                Log.e("TETIRES", "Error saat proses data: ${e.message}", e)
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

    // Di fungsi saveAllResults(), ubah bagian ini:

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

                // ✅ 1. Ambil pengecekan yang ada
                val pengecekan = pengecekanDao.getPengecekanById(currentPengecekanId!!)
                if (pengecekan == null) {
                    addToTerminal("ERROR: Pengecekan tidak ditemukan")
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error: Data pengecekan tidak ditemukan"
                        _cekBanState.value = CekBanState.ERROR
                    }
                    return@launch
                }

                // ✅ 2. VALIDASI ULANG dengan threshold 1.6mm (bukan pakai isWorn dari Python)
                val statusDka = results[PosisiBan.DKA]?.thicknessMm?.let { it < 1.6f }
                val statusDki = results[PosisiBan.DKI]?.thicknessMm?.let { it < 1.6f }
                val statusBka = results[PosisiBan.BKA]?.thicknessMm?.let { it < 1.6f }
                val statusBki = results[PosisiBan.BKI]?.thicknessMm?.let { it < 1.6f }

                val updatedPengecekan = pengecekan.copy(
                    statusDka = statusDka,
                    statusDki = statusDki,
                    statusBka = statusBka,
                    statusBki = statusBki
                )
                pengecekanDao.updatePengecekan(updatedPengecekan)
                addToTerminal("✓ Pengecekan updated")

                // ✅ 3. Ambil atau buat DetailBan
                val existingDetails = detailBanDao.getDetailsByCheckId(currentPengecekanId!!)
                val existingDetail = existingDetails.firstOrNull()

                val detailBan = DetailBan(
                    idDetail = existingDetail?.idDetail ?: 0L,
                    pengecekanId = currentPengecekanId!!,
                    // ✅ Simpan ukuran (thickness_mm dari Python)
                    ukDka = results[PosisiBan.DKA]?.thicknessMm,
                    ukDki = results[PosisiBan.DKI]?.thicknessMm,
                    ukBka = results[PosisiBan.BKA]?.thicknessMm,
                    ukBki = results[PosisiBan.BKI]?.thicknessMm,
                    // ✅ Simpan status aus (REVALIDATED dengan < 1.6mm)
                    statusDka = statusDka,
                    statusDki = statusDki,
                    statusBka = statusBka,
                    statusBki = statusBki
                )

                // ✅ 4. Insert atau update DetailBan
                if (existingDetail == null) {
                    detailBanDao.insertDetailBan(detailBan)
                    addToTerminal("✓ DetailBan inserted")
                } else {
                    detailBanDao.updateDetailBan(detailBan)
                    addToTerminal("✓ DetailBan updated")
                }

                // ✅ 5. Log hasil dengan status yang benar
                addToTerminal("--- SAVED DATA ---")
                results.forEach { (posisi, result) ->
                    val status = if (result.thicknessMm < 1.6f) "AUS ❌" else "OK ✅"
                    addToTerminal("${posisi.label}: ${result.thicknessMm} mm ($status)")
                }
                addToTerminal("✓ Database save complete")

                withContext(Dispatchers.Main) {
                    _cekBanState.value = CekBanState.SAVED
                    _statusMessage.value = "Semua data berhasil disimpan!"
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
    val thicknessMm: Float,
    val isWorn: Boolean,
    val dataCount: Int
)