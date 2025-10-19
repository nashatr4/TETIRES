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
            val newCheck = Pengecekan(busId = busId, tanggalMs = System.currentTimeMillis(), waktuMs = System.currentTimeMillis())
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
                    waktuReadable = DateUtils.formatTime(item.tanggalMs),
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
    fun searchLogs(query: LogQuery): Flow<List<LogItem>> {
        return pengecekanDao.getLast10ChecksAllBus().map { list ->
        list.filter { item ->
                val qRaw = query.searchQuery?.trim()
                if (qRaw == null) return@filter true

                val q = qRaw.lowercase()

                // readable date string (mis. "13 Okt 2025") untuk pengecekan tanggal
                val readable = DateUtils.formatDate(item.tanggalMs).lowercase()

                // ------ 1) DETEKSI STATUS (intent match, bukan substring) ------
                val statusText = if (
                    item.statusDka == true || item.statusDki == true ||
                    item.statusBka == true || item.statusBki == true
                ) "aus" else "tidak aus"

                val statusMatch = when (q) {
                    "aus", "a u s" -> statusText == "aus"
                    "tidak aus", "tidak", "tidakaus", "tidak_a u s" -> statusText == "tidak aus"
                    else -> false // jika query bukan intent status, jangan gunakan substring matching
                }

                // ------ 2) DETEKSI TANGGAL ------
                // day number (e.g. "13")
                val dayMatch = q.matches(Regex("^\\d{1,2}\$")) && readable.contains(q)

                // year (e.g. "2025")
                val yearMatch = q.matches(Regex("^\\d{4}\$")) && readable.contains(q)

                // month names (ID) full & short
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

                // direct readable contains (contoh: user ketik "19 okt 2025" atau "13 okt")
                val readableContains = readable.contains(q)

                val dateMatch = dayMatch || yearMatch || monthMatch || readableContains

                // ------ 3) MATCH NAMA BUS / PLAT + fallback status (if not handled above) ------
                val namaMatch = item.namaBus.contains(q, ignoreCase = true)
                val platMatch = item.platNomor.contains(q, ignoreCase = true)

                // Jika query merupakan intent status -> pakai statusMatch
                // Jika bukan intent status, kita akan match tanggal/nama/plat OR (as fallback) status substring only if user explicitly typed words beyond exact intent
                val matchQuery = when {
                    statusMatch -> true
                    else -> namaMatch || platMatch || dateMatch ||
                            // tambahan: jika user mengetik kata "aus" tapi tidak tepat, kita cek juga full word boundary:
                            // (mis. user mengetik "aus" tapi statusText == "tidak aus" -> tidak akan match karena statusMatch=false)
                            false
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