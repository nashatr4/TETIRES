package com.example.tetires.data.repository

import com.example.tetires.data.local.dao.BusDao
import com.example.tetires.data.local.dao.DetailBanDao
import com.example.tetires.data.local.dao.PengecekanDao
import com.example.tetires.data.local.dao.PengukuranAlurDao
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.local.entity.PengukuranAlur
import com.example.tetires.data.model.*
import com.example.tetires.util.DateUtils
import com.example.tetires.util.TireStatusHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TetiresRepository(
    private val busDao: BusDao,
    private val pengecekanDao: PengecekanDao,
    private val detailBanDao: DetailBanDao,
    private val pengukuranAlurDao: PengukuranAlurDao
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
            listOf(latest.statusDka, latest.statusDki, latest.statusBka, latest.statusBki).all { it != null }
        }

        return if (shouldCreateNew) {
            val newCheck = Pengecekan(
                busId = busId,
                tanggalMs = System.currentTimeMillis(),
                waktuMs = System.currentTimeMillis()
            )
            val newId = pengecekanDao.insertPengecekan(newCheck)

            // 4 Detail Ban untuk 4 Posisi Ban
            val posisiBanList = listOf("DKA", "DKI", "BKA", "BKI")
            val detailBanList = posisiBanList.map { posisi ->
                DetailBan(
                    pengecekanId = newId,
                    posisiBan = posisi,
                    status = null
                )
            }
            val detailIds = detailBanDao.insertAllDetailBan(detailBanList)

            // Buat 4 PengukuranAlur untuk setiap Detail Ban
            val pengukuranList = detailIds.map { detailId ->
                PengukuranAlur(detailBanId = detailId)
            }
            pengukuranAlurDao.insertAllPengukuran(pengukuranList)

            newCheck.copy(idPengecekan = newId)
        } else {
            latest ?: throw IllegalStateException("Gagal mendapatkan pengecekan terbaru")
        }
    }

    suspend fun updateCheckPartial(
        idPengecekan: Long,
        posisi: PosisiBan,
        alurValues: FloatArray
    ): UpdateResult {
        require(alurValues.size == 4) { "alurValues harus berisi 4 nilai" }

        val check = pengecekanDao.getPengecekanById(idPengecekan)
            ?: throw IllegalArgumentException("Pengecekan tidak ditemukan")

        val detail = detailBanDao.getDetailByPosisi(idPengecekan, posisi.name)
            ?: throw IllegalArgumentException("Detail ban $posisi tidak ditemukan")

        val isAus = alurValues.any { it < 1.6f }

        val updatedDetail = detail.copy(status = isAus)
        detailBanDao.updateDetailBan(updatedDetail)

        val existingPengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detail.idDetail)
        val pengukuran = (existingPengukuran ?: PengukuranAlur(detailBanId = detail.idDetail))
            .copy(
                alur1 = alurValues[0],
                alur2 = alurValues[1],
                alur3 = alurValues[2],
                alur4 = alurValues[3]
            )
        pengukuranAlurDao.insertPengukuran(pengukuran)

        val updatedCheck = when (posisi) {
            PosisiBan.DKA -> check.copy(statusDka = isAus)
            PosisiBan.DKI -> check.copy(statusDki = isAus)
            PosisiBan.BKA -> check.copy(statusBka = isAus)
            PosisiBan.BKI -> check.copy(statusBki = isAus)
        }
        pengecekanDao.updatePengecekan(updatedCheck)

        val isComplete = listOf(
            updatedCheck.statusDka,
            updatedCheck.statusDki,
            updatedCheck.statusBka,
            updatedCheck.statusBki
        ).all { it != null }

        return UpdateResult(
            complete = isComplete,
            idCek = idPengecekan,
            statusMessage = if (isAus) "Ban aus (≤1.6mm)" else "Ban aman (>1.6mm)"
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

        val detailList = detailBanDao.getDetailsByCheckId(idCek)
        if (detailList.isEmpty()) return null

        val detailMap = detailList.associateBy { it.posisiBan }

        val pengukuranMap = detailList.associate { detail ->
            detail.posisiBan to pengukuranAlurDao.getPengukuranByDetailBanId(detail.idDetail)
        }

        fun createAlurBan(pengukuran: PengukuranAlur?): AlurBan? {
            return pengukuran?.let {
                AlurBan(
                    alur1 = it.alur1,
                    alur2 = it.alur2,
                    alur3 = it.alur3,
                    alur4 = it.alur4
                )
            }
        }

        return CheckDetail(
            idCek = check.idPengecekan,
            tanggalCek = check.tanggalMs,
            tanggalReadable = DateUtils.formatDate(check.tanggalMs),
            waktuReadable = DateUtils.formatTime(check.waktuMs),
            namaBus = bus.namaBus,
            platNomor = bus.platNomor,

            // Status ban
            statusDka = detailMap["DKA"]?.status ?: false,
            statusDki = detailMap["DKI"]?.status ?: false,
            statusBka = detailMap["BKA"]?.status ?: false,
            statusBki = detailMap["BKI"]?.status ?: false,

            // Rata-rata kedalaman (dari 4 alur)
            alurDka = createAlurBan(pengukuranMap["DKA"]),
            alurDki = createAlurBan(pengukuranMap["DKI"]),
            alurBka = createAlurBan(pengukuranMap["BKA"]),
            alurBki = createAlurBan(pengukuranMap["BKI"]),
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

                // Compute summary status
                val summaryStatus = computeSummaryStatus(
                    item.statusDka, item.statusDki,
                    item.statusBka, item.statusBki
                )

                // Status matching
                val statusMatch = when {
                    q in listOf("aus", "a u s") -> summaryStatus == "Aus"
                    q in listOf("tidak aus", "aman", "tidakaus") -> summaryStatus == "Tidak Aus"
                    else -> false
                }

                // Date matching
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

                // Name/plate matching
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

    suspend fun completeCheck(idCek: Long) {
        val detailList = detailBanDao.getDetailsByCheckId(idCek)
        for (detail in detailList) {
            if (detail.status == null) {
                // Ambil pengukuran untuk ban
                val pengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detail.idDetail)
                if (pengukuran != null) {
                    val alurList = listOfNotNull(
                        pengukuran.alur1,
                        pengukuran.alur2,
                        pengukuran.alur3,
                        pengukuran.alur4
                    )
                    if (alurList.isNotEmpty()) {
                        val isAus = alurList.any { it < 1.6f }
                        detailBanDao.updateDetailBan(detail.copy(status = isAus))
                    }
                }
            }
        }
    }

    // ========== 🔥 LOGIKA STATUS YANG BENAR ==========
    /**
     * Rule (SESUAI PERMINTAAN):
     * 1. Jika SEMUA ban (4 ban) TIDAK AUS (semua false) → "Tidak Aus" ✅
     * 2. Jika ADA MINIMAL 1 ban AUS (minimal 1 true) → "Aus" ❌
     * 3. Jika ada ban belum dicek (ada null) → "Belum Selesai" ⏳
     */
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