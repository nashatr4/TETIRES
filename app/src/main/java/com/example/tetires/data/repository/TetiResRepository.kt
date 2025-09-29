package com.example.tetires.data.repository

import com.example.tetires.data.local.dao.BusDao
import com.example.tetires.data.local.dao.DetailBanDao
import com.example.tetires.data.local.dao.PengecekanDao
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.model.*
import com.example.tetires.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

        return if (latest != null && latest.tanggalMs in startOfDay until endOfDay) {
            latest
        } else {
            val newCheck = Pengecekan(busId = busId, tanggalMs = System.currentTimeMillis())
            val newId = pengecekanDao.insertPengecekan(newCheck)

            detailBanDao.insertDetailBan(DetailBan(pengecekanId = newId))

            newCheck.copy(idPengecekan = newId)
        }
    }

    suspend fun updateCheckPartial(
        idPengecekan: Long,
        posisi: PosisiBan,
        ukuran: Float,
        isAus: Boolean
    ): UpdateResult {
        val check = pengecekanDao.getPengecekanById(idPengecekan)
            ?: throw IllegalArgumentException("Pengecekan not found")

        val detail = detailBanDao.getDetailsByCheckId(idPengecekan).firstOrNull()
            ?: throw IllegalArgumentException("Detail not found")

        val updatedCheck = when (posisi) {
            PosisiBan.DKA -> check.copy(statusDka = isAus)
            PosisiBan.DKI -> check.copy(statusDki = isAus)
            PosisiBan.BKA -> check.copy(statusBka = isAus)
            PosisiBan.BKI -> check.copy(statusBki = isAus)
        }

        val updatedDetail = when (posisi) {
            PosisiBan.DKA -> detail.copy(ukDka = ukuran, statusDka = isAus)
            PosisiBan.DKI -> detail.copy(ukDki = ukuran, statusDki = isAus)
            PosisiBan.BKA -> detail.copy(ukBka = ukuran, statusBka = isAus)
            PosisiBan.BKI -> detail.copy(ukBki = ukuran, statusBki = isAus)
        }

        pengecekanDao.updatePengecekan(updatedCheck)
        detailBanDao.updateDetailBan(updatedDetail)

        val isComplete = listOf(
            updatedCheck.statusDka,
            updatedCheck.statusDki,
            updatedCheck.statusBka,
            updatedCheck.statusBki
        ).all { it != null }

        return UpdateResult(complete = isComplete, idCek = idPengecekan)
    }

    fun getLast10Checks(busId: Long): Flow<List<PengecekanRingkas>> {
        return pengecekanDao.getPengecekanByBus(busId).map { checksWithBus ->
            checksWithBus.take(10).map { item ->
                val isComplete = listOf(
                    item.statusDka, item.statusDki,
                    item.statusBka, item.statusBki
                ).all { it != null }

                PengecekanRingkas(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
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
    fun searchLogs(query: LogQuery): Flow<List<LogItem>> {
        return pengecekanDao.getAllWithBus().map { list ->
            list.filter { item ->
                val matchQuery = query.searchQuery?.let {
                    item.namaBus.contains(it, ignoreCase = true) ||
                            item.platNomor.contains(it, ignoreCase = true)
                } ?: true

                val matchDate = (query.startDate?.let { item.tanggalMs >= it } ?: true) &&
                        (query.endDate?.let { item.tanggalMs <= it } ?: true)

                matchQuery && matchDate
            }.map { item ->
                val summaryStatus = if (
                    item.statusDka == true || item.statusDki == true ||
                    item.statusBka == true || item.statusBki == true
                ) "Aus" else "Tidak Aus"

                LogItem(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
                    namaBus = item.namaBus,
                    platNomor = item.platNomor,
                    summaryStatus = summaryStatus
                )
            }
        }
    }
}