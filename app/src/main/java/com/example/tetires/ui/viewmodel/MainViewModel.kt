package com.example.tetires.ui.viewmodel

import LogItem
import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.PengecekanWithBus
import com.example.tetires.data.model.*
import com.example.tetires.data.repository.TetiresRepository
import com.example.tetires.util.DownloadHelper
import com.example.tetires.util.TireStatusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else { hasBeenHandled = true; content }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    private val repository: TetiresRepository
) : AndroidViewModel(application) {

    private val _buses = MutableStateFlow<List<Bus>>(emptyList())
    val buses: StateFlow<List<Bus>> = _buses.asStateFlow()

    private val _logQuery = MutableStateFlow(LogQuery())
    val recentLogs: StateFlow<List<LogItem>> = _logQuery
        .flatMapLatest { query -> repository.searchLogs(query) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentBusChecks = MutableStateFlow<List<PengecekanRingkas>>(emptyList())
    val currentBusChecks: StateFlow<List<PengecekanRingkas>> = _currentBusChecks.asStateFlow()

    private val _checkDetail = MutableStateFlow<CheckDetail?>(null)
    val checkDetail: StateFlow<CheckDetail?> = _checkDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _busLastChecks = mutableMapOf<Long, MutableStateFlow<PengecekanRingkas?>>()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun showStatusMessage(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private val _startCheckEvent = MutableLiveData<Event<Long>>()
    val startCheckEvent: LiveData<Event<Long>> = _startCheckEvent

    private val _updateCompleteEvent = MutableLiveData<Event<Boolean>>()
    val updateCompleteEvent: LiveData<Event<Boolean>> = _updateCompleteEvent

    private val _busAddedEvent = MutableLiveData<Event<Long>>()
    val busAddedEvent: LiveData<Event<Long>> = _busAddedEvent

    init {
        loadBuses()
    }

    private fun loadBuses() {
        viewModelScope.launch {
            repository.getAllBuses().collect { _buses.value = it }
        }
    }

    fun completeCheck(idCek: Long) {
        viewModelScope.launch {
            try {
                repository.completeCheck(idCek)
                _statusMessage.value = "Pengecekan selesai!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menyelesaikan pengecekan: ${e.message}"
            }
        }
    }

    suspend fun getBusById(busId: Long): Bus? {
        return repository.getBusById(busId)
    }

    fun getLastCheckForBus(busId: Long): StateFlow<PengecekanRingkas?> {
        if (!_busLastChecks.containsKey(busId)) {
            val flow = MutableStateFlow<PengecekanRingkas?>(null)
            _busLastChecks[busId] = flow
            viewModelScope.launch {
                repository.getLast10Checks(busId).collect { checks ->
                    flow.value = checks.firstOrNull()
                }
            }
        }
        return _busLastChecks[busId]!!.asStateFlow()
    }

    fun addBus(nama: String, plat: String) {
        if (nama.isBlank() || plat.isBlank()) {
            _errorMessage.value = "Nama bus dan plat nomor tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val bus = Bus(namaBus = nama, platNomor = plat)
            val result = repository.insertBus(bus)
            result.fold(
                onSuccess = { id ->
                    _busAddedEvent.value = Event(id)
                    _errorMessage.value = null
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "Gagal menambahkan bus"
                }
            )
            _isLoading.value = false
        }
    }

    fun deleteBus(busId: Long) {
        viewModelScope.launch {
            val bus = repository.getBusById(busId)
            if (bus != null) repository.deleteBus(bus)
            else _errorMessage.value = "Bus tidak ditemukan"
        }
    }

    fun deletePengecekan(idCek: Long, busId: Long) {
        viewModelScope.launch {
            val result = repository.deletePengecekanById(idCek)
            result.fold(
                onSuccess = {
                    loadLast10Checks(busId)
                },
                onFailure = { e ->
                    _errorMessage.value = "Gagal menghapus pengecekan: ${e.message}"
                }
            )
        }
    }

    fun startCheck(busId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.startOrGetOpenCheck(busId).idPengecekan
                _startCheckEvent.value = Event(id)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memulai pengecekan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCheckPartial(idCek: Long, posisi: PosisiBan, alurValues: FloatArray) {
        if (alurValues.size != 4) {
            _errorMessage.value = "Harus 4 nilai alur"
            return
        }

        if (alurValues.any { !TireStatusHelper.isValidUkuran(it) }) {
            _errorMessage.value = "Ada nilai alur ban yang tidak valid"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.updateCheckPartial(idCek, posisi, alurValues)
                _statusMessage.value = result.statusMessage
                _updateCompleteEvent.value = Event(result.complete)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update pengecekan: ${e.message}"
                _statusMessage.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchLogs(query: String? = null, startDate: Long? = null, endDate: Long? = null) {
        _logQuery.value = LogQuery(
            searchQuery = query?.takeIf { it.isNotBlank() },
            startDate = startDate,
            endDate = endDate
        )
    }

    fun clearSearch() {
        _logQuery.value = LogQuery()
    }

    fun loadLast10Checks(busId: Long?) {
        if (busId == null) return
        viewModelScope.launch {
            repository.getLast10Checks(busId).collect { checks ->
                _currentBusChecks.value = checks
            }
        }
    }

    fun loadCheckDetail(idCek: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _checkDetail.value = repository.getCheckDetail(idCek)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat detail: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    // ===== DOWNLOAD FUNCTIONS =====

    /**
     * Download riwayat pengecekan sebagai CSV file dan langsung buka
     * Method 1: Save ke Downloads + Auto Open
     */
    fun downloadHistory(context: Context, logs: List<PengecekanRingkas>, busName: String? = null) {
        DownloadHelper.downloadHistoryAsCSV(context, logs, busName)
    }

    /**
     * Alternative Method: Share CSV (Lebih reliable karena tidak perlu storage permission)
     * User bisa pilih: Save ke Drive, Buka dengan Excel, Share via WhatsApp, dll
     */
    fun downloadAndShareHistory(context: Context, logs: List<PengecekanRingkas>, busName: String? = null) {
        DownloadHelper.downloadAndShareCSV(context, logs, busName)
    }

    /**
     * Download riwayat detail (dengan format lengkap)
     * Include semua informasi status ban per posisi
     */
    fun downloadDetailedHistory(context: Context, busId: Long) {
        viewModelScope.launch {
            try {
                val logs = repository.getLast10Checks(busId).first()
                val bus = repository.getBusById(busId)
                DownloadHelper.downloadDetailedHistory(context, logs, bus?.namaBus)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal download: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ===== TEST FUNCTION =====

    fun testUpdateFlow() {
        viewModelScope.launch {
            try {
                val result = repository.updateCheckPartial(
                    idPengecekan = 1L,
                    posisi = PosisiBan.DKA,
                    alurValues = floatArrayOf(2.5f, 1.3f, 2.2f, 1.8f)
                )
                Log.d("TEST_RESULT", "Success=${result.complete}, Message=${result.statusMessage}")
            } catch (e: Exception) {
                Log.e("TEST_RESULT", "Gagal: ${e.message}")
            }
        }
    }
}
