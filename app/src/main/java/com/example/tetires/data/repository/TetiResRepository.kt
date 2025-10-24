package com.example.tetires.data.repository

import com.example.tetires.data.local.dao.BusDao
import com.example.tetires.data.local.dao.DetailBanDao
import com.example.tetires.data.local.dao.PengecekanDao
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.model.*
import com.example.tetires.util.DateUtils
import com.example.tetires.util.TireStatusHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TetiresRepository(
    private val busDao: BusDao,
    private val pengecekanDao: PengecekanDao,
    private val detailBanDao: DetailBanDao
) {

    // ========== BUS ==========
    fun getAllBuses(): Flow<List<Bus>> = busDao.getAllBus()

    suspend fun insertBus(bus: Bus): Result<Long> {
        return try {
            val existing = busDao.getBusByPlat(bus.platNomor)
            if (existing != null) {
                Result.failure(Exception("Plat nomor sudah terdaftar"))
            } else {
                Result.success(busDao.insertBus(bus))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBus(bus: Bus) = busDao.deleteBus(bus)

    suspend fun getBusById(busId: Long): Bus? = busDao.getBusById(busId)

    // ========== PENGECEKAN ==========
    suspend fun startOrGetOpenCheck(busId: Long): Pengecekan {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = (calendar.apply { add(Calendar.DAY_OF_MONTH, 1) }).timeInMillis

        val latest = pengecekanDao.getLatestPengecekanForBus(busId)

        val shouldCreateNew = if (latest == null) {
            true
        } else {
            val detailList = detailBanDao.getDetailsByCheckId(latest.idPengecekan)
            val lastDetail = detailList.firstOrNull()
            val isComplete = lastDetail?.let {
                listOf(it.ukDka, it.ukDki, it.ukBka, it.ukBki).all { uk -> uk != null }
            } ?: false
            isComplete
        }

        return if (shouldCreateNew) {
            val newCheck = Pengecekan(
                busId = busId,
                tanggalMs = System.currentTimeMillis(),
                waktuMs = System.currentTimeMillis()
            )
            val newId = pengecekanDao.insertPengecekan(newCheck)
            detailBanDao.insertDetailBan(DetailBan(pengecekanId = newId))
            newCheck.copy(idPengecekan = newId)
        } else {
            // 💡 di sini tambahin null handling
            latest ?: throw IllegalStateException("Gagal mendapatkan pengecekan terbaru untuk busId=$busId")
        }
    }


    /**
     * Update pengecekan dengan ukuran tapak ban.
     * Status aus/tidak aus ditentukan OTOMATIS berdasarkan ukuran (threshold 1.6mm).
     * Data disinkronkan ke tabel pengecekan DAN detail_ban.
     */
    suspend fun updateCheckPartial(
        idPengecekan: Long,
        posisi: PosisiBan,
        ukuran: Float
    ): UpdateResult {
        if (!TireStatusHelper.isValidUkuran(ukuran)) throw IllegalArgumentException("Ukuran tidak valid")

        val check = pengecekanDao.getPengecekanById(idPengecekan) ?: throw IllegalArgumentException("Pengecekan not found")
        val detail = detailBanDao.getDetailsByCheckId(idPengecekan).firstOrNull() ?: throw IllegalArgumentException("Detail not found")

        val isAus = TireStatusHelper.isAus(ukuran) ?: false

        val updatedCheck = when(posisi){
            PosisiBan.DKA -> check.copy(statusDka = isAus)
            PosisiBan.DKI -> check.copy(statusDki = isAus)
            PosisiBan.BKA -> check.copy(statusBka = isAus)
            PosisiBan.BKI -> check.copy(statusBki = isAus)
        }

        val updatedDetail = when(posisi){
            PosisiBan.DKA -> detail.copy(ukDka=ukuran,statusDka=isAus)
            PosisiBan.DKI -> detail.copy(ukDki=ukuran,statusDki=isAus)
            PosisiBan.BKA -> detail.copy(ukBka=ukuran,statusBka=isAus)
            PosisiBan.BKI -> detail.copy(ukBki=ukuran,statusBki=isAus)
        }

        pengecekanDao.updatePengecekan(updatedCheck)
        detailBanDao.updateDetailBan(updatedDetail)

        val isComplete = listOf(updatedCheck.statusDka, updatedCheck.statusDki, updatedCheck.statusBka, updatedCheck.statusBki).all{ it!=null }

        return UpdateResult(
            complete = isComplete,
            idCek = idPengecekan,
            statusMessage = if(isAus) "Ban aus (≤1.6mm)" else "Ban tidak aus (>1.6mm)"
        )
    }

    fun getLast10Checks(busId: Long): Flow<List<PengecekanRingkas>> {
        return pengecekanDao.getLast10Checks(busId).map { checksWithBus ->
            checksWithBus.map { item ->
                val isComplete = listOf(
                    item.statusDka, item.statusDki,
                    item.statusBka, item.statusBki
                ).all { it != null }

                PengecekanRingkas(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
                    waktuReadable = DateUtils.formatTime(item.waktuMs),
                    namaBus = item.namaBus,
                    platNomor = item.platNomor,
                    statusDka = item.statusDka,
                    statusDki = item.statusDki,
                    statusBka = item.statusBka,
                    statusBki = item.statusBki,
                    summaryStatus = if (isComplete) "Selesai" else "Belum Selesai"
                )
            }
        }
    }

    suspend fun getCheckDetail(idCek: Long): CheckDetail? {
        val check = pengecekanDao.getPengecekanById(idCek) ?: return null
        val bus = busDao.getBusById(check.busId) ?: return null
        val detail = detailBanDao.getDetailsByCheckId(idCek).firstOrNull() ?: return null

        return CheckDetail(
            idCek = check.idPengecekan,
            tanggalCek = check.tanggalMs,
            tanggalReadable = DateUtils.formatDate(check.tanggalMs),
            waktuReadable = DateUtils.formatTime(check.tanggalMs),
            namaBus = bus.namaBus,
            platNomor = bus.platNomor,
            statusDka = detail.statusDka ?: false,
            statusDki = detail.statusDki ?: false,
            statusBka = detail.statusBka ?: false,
            statusBki = detail.statusBki ?: false,
            ukDka = detail.ukDka ?: 0f,
            ukDki = detail.ukDki ?: 0f,
            ukBka = detail.ukBka ?: 0f,
            ukBki = detail.ukBki ?: 0f
        )
    }

    suspend fun deletePengecekanById(id: Long): Result<Unit> {
        return try {
            // Hapus detail ban dulu (foreign key)
            val details = detailBanDao.getDetailsByCheckId(id)
            details.forEach { detailBanDao.deleteDetailBan(it) }

            // Baru hapus pengecekan
            pengecekanDao.deletePengecekanById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== LOG ==========
    // ========== BUS FILTER / SEARCH ==========
    fun searchBuses(
        query: String?,
        ausCount: Int? = null // 1, 2, 3, 4, atau 0 (tidak ada ban aus)
    ): Flow<List<PengecekanRingkas>> {
        return pengecekanDao.getLast10ChecksAllBus().map { list ->
            list.filter { item ->
                val q = query?.trim()?.lowercase() ?: ""

                val nameMatch = item.namaBus.lowercase().contains(q)
                val plateMatch = item.platNomor.lowercase().contains(q)

                // hitung jumlah ban aus
                val ausTotal = listOf(
                    item.statusDka,
                    item.statusDki,
                    item.statusBka,
                    item.statusBki
                ).count { it == true }

                // kalau ausCount != null, hanya tampilkan yang sesuai jumlahnya
                val ausMatch = ausCount?.let { ausTotal == it } ?: true

                val queryMatch = if (q.isNotEmpty()) {
                    nameMatch || plateMatch ||
                            DateUtils.formatDate(item.tanggalMs).lowercase().contains(q)
                } else true

                queryMatch && ausMatch
            }.map { item ->
                val summaryStatus = when (
                    listOf(item.statusDka, item.statusDki, item.statusBka, item.statusBki).count { it == true }
                ) {
                    0 -> "Tidak Aus"
                    1 -> "1 Ban Aus"
                    2 -> "2 Ban Aus"
                    3 -> "3 Ban Aus"
                    else -> "4 Ban Aus"
                }

                PengecekanRingkas(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
                    waktuReadable = DateUtils.formatTime(item.waktuMs),
                    namaBus = item.namaBus,
                    platNomor = item.platNomor,
                    statusDka = item.statusDka,
                    statusDki = item.statusDki,
                    statusBka = item.statusBka,
                    statusBki = item.statusBki,
                    summaryStatus = summaryStatus
                )
            }
        }
    }

    suspend fun completeCheck(idCek: Long) {
        val detailList = detailBanDao.getDetailsByCheckId(idCek)
        val updatedList = detailList.map { detail ->
            val updatedDetail = detail.copy(
                statusDka = detail.statusDka ?: (detail.ukDka?.let { it <= 1.6f } ?: false),
                statusDki = detail.statusDki ?: (detail.ukDki?.let { it <= 1.6f } ?: false),
                statusBka = detail.statusBka ?: (detail.ukBka?.let { it <= 1.6f } ?: false),
                statusBki = detail.statusBki ?: (detail.ukBki?.let { it <= 1.6f } ?: false)
            )
            updatedDetail
        }
        detailBanDao.updateDetailBan(updatedList)
    }

    suspend fun updateAndCompleteCheck(
        idPengecekan: Long,
        updates: Map<PosisiBan, Float>
    ) {
        val check = pengecekanDao.getPengecekanById(idPengecekan)
            ?: throw IllegalArgumentException("Pengecekan not found")
        val detail = detailBanDao.getDetailsByCheckId(idPengecekan).firstOrNull()
            ?: throw IllegalArgumentException("Detail not found")

        val updatedDetail = updates.entries.fold(detail) { acc, (posisi, ukuran) ->
            val isAus = TireStatusHelper.isAus(ukuran) ?: false
            when (posisi) {
                PosisiBan.DKA -> acc.copy(ukDka = ukuran, statusDka = isAus)
                PosisiBan.DKI -> acc.copy(ukDki = ukuran, statusDki = isAus)
                PosisiBan.BKA -> acc.copy(ukBka = ukuran, statusBka = isAus)
                PosisiBan.BKI -> acc.copy(ukBki = ukuran, statusBki = isAus)
            }
        }

        detailBanDao.updateDetailBan(updatedDetail)

        // update pengecekan
        val updatedCheck = check.copy(
            statusDka = updatedDetail.statusDka,
            statusDki = updatedDetail.statusDki,
            statusBka = updatedDetail.statusBka,
            statusBki = updatedDetail.statusBki
        )
        pengecekanDao.updatePengecekan(updatedCheck)
    }


    fun searchLogs(query: LogQuery): Flow<List<LogItem>> {
        return pengecekanDao.getLast10ChecksAllBus().map { list ->
            list.filter { item ->
                val qRaw = query.searchQuery?.trim()
                if (qRaw == null) return@filter true

                val q = qRaw.lowercase()

                // readable date string
                val readable = DateUtils.formatDate(item.tanggalMs).lowercase()

                // ------ 1) DETEKSI STATUS (intent match) ------
                val statusText = if (
                    item.statusDka == true || item.statusDki == true ||
                    item.statusBka == true || item.statusBki == true
                ) "aus" else "tidak aus"

                val statusMatch = when (q) {
                    "aus", "a u s" -> statusText == "aus"
                    "tidak aus", "tidak", "tidakaus", "tidak_a u s" -> statusText == "tidak aus"
                    else -> false
                }

                // ------ 2) DETEKSI TANGGAL ------
                val dayMatch = q.matches(Regex("^\\d{1,2}\$")) && readable.contains(q)
                val yearMatch = q.matches(Regex("^\\d{4}\$")) && readable.contains(q)

                val monthsFull = listOf(
                    "januari","februari","maret","april","mei","juni",
                    "juli","agustus","september","oktober","november","desember"
                )
                val monthsShort = listOf(
                    "jan","feb","mar","apr","mei","jun","jul","agu","sep","okt","nov","des"
                )

                val monthQueryFound = monthsFull.any { it in q } || monthsShort.any { it in q }
                val monthInReadable = monthsShort.any { it in readable } || monthsFull.any { it in readable }
                val monthMatch = monthQueryFound && monthInReadable

                val readableContains = readable.contains(q)
                val dateMatch = dayMatch || yearMatch || monthMatch || readableContains

                // ------ 3) MATCH NAMA BUS / PLAT ------
                val namaMatch = item.namaBus.contains(q, ignoreCase = true)
                val platMatch = item.platNomor.contains(q, ignoreCase = true)

                val matchQuery = when {
                    statusMatch -> true
                    else -> namaMatch || platMatch || dateMatch
                }

                matchQuery
            }.map { item ->
                val summaryStatus = if (
                    item.statusDka == true || item.statusDki == true ||
                    item.statusBka == true || item.statusBki == true
                ) "Aus" else "Tidak Aus"

                LogItem(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
                    waktuReadable = DateUtils.formatTime(item.tanggalMs),
                    namaBus = item.namaBus,
                    platNomor = item.platNomor,
                    summaryStatus = summaryStatus
                )
            }
        }
    }
}