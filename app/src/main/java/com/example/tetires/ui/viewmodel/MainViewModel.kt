package com.example.tetires.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.PengukuranAlur
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

    // ✅ FIXED: Parameter pakai idCek
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

    // ✅ FIXED: Parameter pakai idCek
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

    // ✅ FIXED: Parameter pakai idCek
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

    // ✅ FIXED: Parameter pakai idCek
    fun loadCheckDetail(idCek: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            _checkDetail.value = null
            Log.d("DetailPengecekan", "State reset, loading detail for idCek=$idCek")

            try {
                Log.d("DetailPengecekan", "Loading detail for idCek=$idCek")
                val detail = repository.getCheckDetail(idCek)

                if (detail != null) {
                    _checkDetail.value = detail
                    Log.d("DetailPengecekan", "Detail loaded successfully: $detail")
                } else {
                    _errorMessage.value = "Data detail tidak ditemukan."
                    Log.e("DetailPengecekan", "No detail found for idCek=$idCek")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat detail: ${e.localizedMessage}"
                Log.e("DetailPengecekan", "Error loading detail", e)
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

    fun downloadHistory(context: Context, busId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val logs = repository.getLast10Checks(busId).first()

                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pengecekanIds = logs.map { it.idCek }
                val pengukuranMap = repository.getPengukuranMapByPengecekanIdsForExport(pengecekanIds)

                val bus = repository.getBusById(busId)

                DownloadHelper.downloadHistoryAsCSV(
                    context = context,
                    logs = logs,
                    pengukuranMap = pengukuranMap,
                    busName = bus?.namaBus
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error downloading history", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal download: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadAndShareHistory(context: Context, busId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val logs = repository.getLast10Checks(busId).first()

                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pengecekanIds = logs.map { it.idCek }
                val pengukuranMap = repository.getPengukuranMapByPengecekanIdsForExport(pengecekanIds)

                val bus = repository.getBusById(busId)

                DownloadHelper.downloadAndShareCSV(
                    context = context,
                    logs = logs,
                    pengukuranMap = pengukuranMap,
                    busName = bus?.namaBus
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sharing history", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal share: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadDetailedHistory(context: Context, busId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val logs = repository.getLast10Checks(busId).first()

                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pengecekanIds = logs.map { it.idCek }
                val pengukuranMap = repository.getPengukuranMapByPengecekanIdsForExport(pengecekanIds)

                val bus = repository.getBusById(busId)

                DownloadHelper.downloadDetailedHistory(
                    context = context,
                    logs = logs,
                    pengukuranMap = pengukuranMap,
                    busName = bus?.namaBus
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error downloading detailed history", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal download detail: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportHistory(context: Context, logs: List<PengecekanRingkas>, busName: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pengecekanIds = logs.map { it.idCek }
                val pengukuranMap = repository.getPengukuranMapByPengecekanIdsForExport(pengecekanIds)

                Log.d("MainViewModel", "Exporting ${logs.size} logs with ${pengukuranMap.size} pengukuran entries")

                DownloadHelper.downloadHistoryAsCSV(
                    context = context,
                    logs = logs,
                    pengukuranMap = pengukuranMap,
                    busName = busName
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error exporting history", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal export: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportAndShareHistory(context: Context, logs: List<PengecekanRingkas>, busName: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (logs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tidak ada riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val pengecekanIds = logs.map { it.idCek }
                val pengukuranMap = repository.getPengukuranMapByPengecekanIdsForExport(pengecekanIds)

                Log.d("MainViewModel", "Sharing ${logs.size} logs with ${pengukuranMap.size} pengukuran entries")

                DownloadHelper.downloadAndShareCSV(
                    context = context,
                    logs = logs,
                    pengukuranMap = pengukuranMap,
                    busName = busName
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sharing history", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Gagal share: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

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