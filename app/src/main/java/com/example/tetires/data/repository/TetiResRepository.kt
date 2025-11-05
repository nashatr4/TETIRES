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
        val latest = pengecekanDao.getLatestPengecekanForBus(busId)
        val shouldCreateNew = if (latest == null) true
        else {
            val detailList = detailBanDao.getDetailsByCheckId(latest.idPengecekan)
            val lastDetail = detailList.firstOrNull()
            lastDetail?.let {
                listOf(it.ukDka, it.ukDki, it.ukBka, it.ukBki).all { uk -> uk != null }
            } ?: false
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
            latest ?: throw IllegalStateException("Gagal mendapatkan pengecekan terbaru")
        }
    }

    // ✅ FIX: Ubah parameter name dan return type
    suspend fun updateCheckPartial(idPengecekan: Long, posisi: PosisiBan, ukuran: Float): UpdateCheckResult {
        val detail = detailBanDao.getDetailBanById(idPengecekan)

        val updated = (detail ?: DetailBan(pengecekanId = idPengecekan)).copy(
            ukDka = if (posisi == PosisiBan.DKA) ukuran else detail?.ukDka,
            ukDki = if (posisi == PosisiBan.DKI) ukuran else detail?.ukDki,
            ukBka = if (posisi == PosisiBan.BKA) ukuran else detail?.ukBka,
            ukBki = if (posisi == PosisiBan.BKI) ukuran else detail?.ukBki,
            // ✅ Langsung update status saat input (< 1.6mm = aus)
            statusDka = if (posisi == PosisiBan.DKA) (ukuran < 1.6f) else detail?.statusDka,
            statusDki = if (posisi == PosisiBan.DKI) (ukuran < 1.6f) else detail?.statusDki,
            statusBka = if (posisi == PosisiBan.BKA) (ukuran < 1.6f) else detail?.statusBka,
            statusBki = if (posisi == PosisiBan.BKI) (ukuran < 1.6f) else detail?.statusBki
        )

        if (detail == null)
            detailBanDao.insertDetailBan(updated)
        else
            detailBanDao.updateDetailBan(updated)

        // ✅ Update status di tabel Pengecekan juga
        syncStatusWithDetail(idPengecekan)

        // ✅ Cek apakah semua ban sudah diisi
        val complete = listOf(
            updated.ukDka,
            updated.ukDki,
            updated.ukBka,
            updated.ukBki
        ).all { it != null }

        // ✅ Buat status message
        val statusText = if (ukuran < 1.6f) "AUS ❌" else "OK ✅"
        val statusMessage = "Update ${posisi.label}: ${ukuran}mm ($statusText)"

        return UpdateCheckResult(
            complete = complete,
            statusMessage = statusMessage
        )
    }

    fun getLast10Checks(busId: Long): Flow<List<PengecekanRingkas>> {
        return pengecekanDao.getLast10Checks(busId).map { checksWithBus ->
            checksWithBus.map { item ->
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
                    summaryStatus = computeSummaryStatus(
                        item.statusDka, item.statusDki,
                        item.statusBka, item.statusBki
                    )
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
            waktuReadable = DateUtils.formatTime(check.waktuMs),
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
            val details = detailBanDao.getDetailsByCheckId(id)
            details.forEach { detailBanDao.deleteDetailBan(it) }
            pengecekanDao.deletePengecekanById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchLogs(query: LogQuery): Flow<List<LogItem>> {
        return pengecekanDao.getLast10ChecksAllBus().map { list ->
            list.filter { item ->
                val qRaw = query.searchQuery?.trim()
                if (qRaw == null) return@filter true

                val q = qRaw.lowercase()
                val readable = DateUtils.formatDate(item.tanggalMs).lowercase()

                val summaryStatus = computeSummaryStatus(
                    item.statusDka, item.statusDki,
                    item.statusBka, item.statusBki
                )

                val statusMatch = when {
                    q in listOf("aus", "a u s") -> summaryStatus == "Aus"
                    q in listOf("tidak aus", "aman", "tidakaus") -> summaryStatus == "Tidak Aus"
                    else -> false
                }

                val dayMatch = q.matches(Regex("^\\d{1,2}\$")) && readable.contains(q)
                val yearMatch = q.matches(Regex("^\\d{4}\$")) && readable.contains(q)

                val monthsFull = listOf(
                    "januari","februari","maret","april","mei","juni",
                    "juli","agustus","september","oktober","november","desember"
                )
                val monthsShort = listOf(
                    "jan","feb","mar","apr","mei","jun","jul","agu","sep","okt","nov","des"
                )

                val monthMatch = (monthsFull.any { it in q } || monthsShort.any { it in q }) &&
                        (monthsShort.any { it in readable } || monthsFull.any { it in readable })

                val dateMatch = dayMatch || yearMatch || monthMatch || readable.contains(q)

                val namaMatch = item.namaBus.contains(q, ignoreCase = true)
                val platMatch = item.platNomor.contains(q, ignoreCase = true)

                statusMatch || namaMatch || platMatch || dateMatch
            }.map { item ->
                LogItem(
                    idCek = item.idPengecekan,
                    tanggalCek = item.tanggalMs,
                    tanggalReadable = DateUtils.formatDate(item.tanggalMs),
                    waktuReadable = DateUtils.formatTime(item.tanggalMs),
                    namaBus = item.namaBus,
                    platNomor = item.platNomor,
                    summaryStatus = computeSummaryStatus(
                        item.statusDka, item.statusDki,
                        item.statusBka, item.statusBki
                    )
                )
            }
        }
    }

    // ✅ FIX: Gunakan < 1.6f (bukan <=)
    suspend fun completeCheck(idCek: Long) {
        val detailList = detailBanDao.getDetailsByCheckId(idCek)
        val updatedList = detailList.map { detail ->
            detail.copy(
                statusDka = detail.statusDka ?: (detail.ukDka?.let { it < 1.6f } ?: false),
                statusDki = detail.statusDki ?: (detail.ukDki?.let { it < 1.6f } ?: false),
                statusBka = detail.statusBka ?: (detail.ukBka?.let { it < 1.6f } ?: false),
                statusBki = detail.statusBki ?: (detail.ukBki?.let { it < 1.6f } ?: false)
            )
        }

        for (detail in updatedList) {
            detailBanDao.updateDetailBan(detail)
        }
    }

    suspend fun syncStatusWithDetail(idPengecekan: Long) {
        val detail = detailBanDao.getDetailBanById(idPengecekan) ?: return
        val pengecekan = pengecekanDao.getPengecekanById(idPengecekan) ?: return

        val updated = pengecekan.copy(
            statusDka = TireStatusHelper.isAus(detail.ukDka),
            statusDki = TireStatusHelper.isAus(detail.ukDki),
            statusBka = TireStatusHelper.isAus(detail.ukBka),
            statusBki = TireStatusHelper.isAus(detail.ukBki)
        )

        pengecekanDao.updatePengecekan(updated)
    }

    // ========== LOGIKA STATUS ==========
    private fun computeSummaryStatus(
        dka: Boolean?,
        dki: Boolean?,
        bka: Boolean?,
        bki: Boolean?
    ): String {
        val list = listOf(dka, dki, bka, bki)

        // ⏳ Jika ada yang null → belum selesai
        if (list.any { it == null }) {
            return "Belum Selesai"
        }

        // ❌ Jika ada minimal 1 ban aus (true) → AUS
        if (list.any { it == true }) {
            return "Aus"
        }

        // ✅ Jika SEMUA ban tidak aus (semua false) → TIDAK AUS
        return "Tidak Aus"
    }
}