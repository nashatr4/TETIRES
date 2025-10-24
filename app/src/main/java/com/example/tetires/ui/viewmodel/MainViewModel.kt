package com.example.tetires.ui.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.PengecekanWithBus
import com.example.tetires.data.model.*
import com.example.tetires.data.repository.TetiresRepository
import com.example.tetires.util.TireStatusHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else { hasBeenHandled = true; content }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: TetiresRepository
) : ViewModel() {

    // ========= STATEFLOW =========
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

    // 🔥 NEW: Status message untuk feedback ke user
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // ========= LIVE DATA (One-time event) =========
    private val _startCheckEvent = MutableLiveData<Event<Long>>()
    val startCheckEvent: LiveData<Event<Long>> = _startCheckEvent

    private val _updateCompleteEvent = MutableLiveData<Event<Boolean>>()
    val updateCompleteEvent: LiveData<Event<Boolean>> = _updateCompleteEvent

    private val _busAddedEvent = MutableLiveData<Event<Long>>()
    val busAddedEvent: LiveData<Event<Long>> = _busAddedEvent

    init { loadBuses() }

    // ========= BUS =========
    private fun loadBuses() {
        viewModelScope.launch { repository.getAllBuses().collect { _buses.value = it } }
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
                    loadLast10Checks(busId) // refresh list cek untuk bus ini
                },
                onFailure = { e ->
                    _errorMessage.value = "Gagal menghapus pengecekan: ${e.message}"
                }
            )
        }
    }

    // ========= PENGECEKAN =========
    fun startCheck(busId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _startCheckEvent.value =
                    Event(repository.startOrGetOpenCheck(busId).idPengecekan)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memulai pengecekan: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * 🔥 UPDATE: Tidak perlu parameter isAus lagi.
     * Status ditentukan OTOMATIS dari ukuran (threshold 1.6mm).
     */
    fun updateCheckPartial(idCek: Long, posisi: PosisiBan, ukuran: Float) {
        // Validasi ukuran
        if (!TireStatusHelper.isValidUkuran(ukuran)) {
            _errorMessage.value = "Ukuran tidak valid: $ukuran mm (harus 0-50mm)"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // langsung kirim enum posisi
                val result = repository.updateCheckPartial(idCek, posisi, ukuran)

                // update feedback ke UI
                _statusMessage.value = result.statusMessage
                _updateCompleteEvent.value = Event(result.complete)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Gagal update pengecekan: ${e.message}"
                _statusMessage.value = null
            }
            _isLoading.value = false
        }
    }


    // ========= LOG / SEARCH =========
    fun searchLogs(query: String? = null, startDate: Long? = null, endDate: Long? = null) {
        _logQuery.value = LogQuery(
            searchQuery = query?.takeIf { it.isNotBlank() },
            startDate = startDate,
            endDate = endDate
        )
    }

    fun clearSearch() { _logQuery.value = LogQuery() }

    // ========= RIWAYAT =========
    fun loadLast10Checks(busId: Long?) {
        if (busId == null) return
        viewModelScope.launch {
            repository.getLast10Checks(busId).collect { checks ->
                _currentBusChecks.value = checks
            }
        }
    }

    private fun PengecekanWithBus.toPengecekanRingkas(): PengecekanRingkas {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val readableDate = dateFormat.format(Date(tanggalMs))

        val dka = statusDka == true
        val dki = statusDki == true
        val bka = statusBka == true
        val bki = statusBki == true

        return PengecekanRingkas(
            idCek = idPengecekan,
            tanggalCek = tanggalMs,
            tanggalReadable = readableDate,
            waktuReadable = readableDate,
            namaBus = namaBus,
            platNomor = platNomor,
            statusDka = dka,
            statusDki = dki,
            statusBka = bka,
            statusBki = bki,
            summaryStatus = if (dka || dki || bka || bki) "Aus" else "Aman"
        )
    }

    // ========= DETAIL =========
    fun loadCheckDetail(idCek: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _checkDetail.value = repository.getCheckDetail(idCek)
            }
            catch (e: Exception) {
                _errorMessage.value = "Gagal memuat detail: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    fun testUpdateFlow() {
        viewModelScope.launch {
            try {
                val result = repository.updateCheckPartial(
                    idPengecekan = 1L,
                    posisi = PosisiBan.DKA,
                    ukuran = 1.4f
                )
                Log.d("TEST_RESULT", "Success=${result.complete}, Message=${result.statusMessage ?: "null"}")
            } catch (e: Exception) {
                Log.e("TEST_RESULT", "Gagal: ${e.message}")
            }
        }
    }

}