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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.tetires.util.DownloadHelper

private const val TAG = "TetiresRepository"

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
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            require(alurValues.size == 4) { "Must provide exactly 4 groove values" }

            // Hitung status aus berdasarkan nilai minimum
            val isAus = TireStatusHelper.isAusFromAlur(alurValues)

            // Cari/buat DetailBan
            val detailBan = detailBanDao.getDetailByPosisi(idPengecekan, posisi.name)
            val detailId = if (detailBan == null) {
                val newDetail = DetailBan(
                    pengecekanId = idPengecekan,
                    posisiBan = posisi.name,
                    status = isAus
                )
                detailBanDao.insertDetailBan(newDetail)
            } else {
                detailBanDao.updateDetailBan(detailBan.copy(status = isAus))
                detailBan.idDetail
            }

            // Simpan/update 4 alur
            val existingPengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detailId)
            if (existingPengukuran == null) {
                pengukuranAlurDao.insertPengukuran(
                    PengukuranAlur(
                        detailBanId = detailId,
                        alur1 = alurValues[0],
                        alur2 = alurValues[1],
                        alur3 = alurValues[2],
                        alur4 = alurValues[3]
                    )
                )
            } else {
                pengukuranAlurDao.updatePengukuran(
                    existingPengukuran.copy(
                        alur1 = alurValues[0],
                        alur2 = alurValues[1],
                        alur3 = alurValues[2],
                        alur4 = alurValues[3]
                    )
                )
            }

            // Update summary di Pengecekan
            val pengecekan = pengecekanDao.getPengecekanById(idPengecekan)
            if (pengecekan != null) {
                val updated = when (posisi) {
                    PosisiBan.DKA -> pengecekan.copy(statusDka = isAus)
                    PosisiBan.DKI -> pengecekan.copy(statusDki = isAus)
                    PosisiBan.BKA -> pengecekan.copy(statusBka = isAus)
                    PosisiBan.BKI -> pengecekan.copy(statusBki = isAus)
                }
                pengecekanDao.updatePengecekan(updated)

                // Cek apakah semua ban sudah dicek
                val allChecked = listOf(
                    updated.statusDka,
                    updated.statusDki,
                    updated.statusBka,
                    updated.statusBki
                ).all { it != null }

                val minAlur = alurValues.minOrNull() ?: 0f
                val statusText = if (isAus) "AUS (< 1.6mm)" else "AMAN (≥ 1.6mm)"

                UpdateResult(
                    complete = allChecked,
                    statusMessage = "${posisi.label}: Min ${String.format("%.1f", minAlur)}mm → $statusText"
                )
            } else {
                UpdateResult(false, "Pengecekan tidak ditemukan")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateCheckPartial failed", e)
            UpdateResult(false, "Error: ${e.message}")
        }
    }

    fun getLast10Checks(busId: Long): Flow<List<PengecekanRingkas>> {
        return pengecekanDao.getLast10Checks(busId)
            .map { checksWithBusList ->
                checksWithBusList.filter { check ->
                    listOf(check.statusDka, check.statusDki, check.statusBka, check.statusBki)
                        .any { it != null }
                }
            }
            .map { filteredList ->
                filteredList.map { item ->
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

    /**
     * ✅ FIXED: getCheckDetail dengan logging detail dan error handling lebih baik
     */
    suspend fun getCheckDetail(idCek: Long): CheckDetail? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== START getCheckDetail for idCek=$idCek ==========")

            // 1. Ambil Pengecekan
            val check = pengecekanDao.getPengecekanById(idCek)
            if (check == null) {
                Log.e(TAG, "❌ Pengecekan tidak ditemukan untuk idCek=$idCek")
                return@withContext null
            }
            Log.d(TAG, "✅ Pengecekan ditemukan: $check")

            // 2. Ambil Bus
            val bus = busDao.getBusById(check.busId)
            if (bus == null) {
                Log.e(TAG, "❌ Bus tidak ditemukan untuk busId=${check.busId}")
                return@withContext null
            }
            Log.d(TAG, "✅ Bus ditemukan: ${bus.namaBus} (${bus.platNomor})")

            // 3. Ambil semua DetailBan untuk pengecekan ini
            val detailList = detailBanDao.getDetailsByCheckId(idCek)
            Log.d(TAG, "📋 DetailBan count: ${detailList.size}")

            if (detailList.isEmpty()) {
                Log.e(TAG, "❌ Tidak ada DetailBan untuk idCek=$idCek")
                return@withContext null
            }

            // Log semua detail ban
            detailList.forEach { detail ->
                Log.d(TAG, "  - DetailBan: posisi=${detail.posisiBan}, status=${detail.status}, idDetail=${detail.idDetail}")
            }

            // 4. Buat map posisi -> DetailBan
            val detailMap = detailList.associateBy { it.posisiBan }

            // 5. Ambil semua PengukuranAlur
            val pengukuranMap = mutableMapOf<String, PengukuranAlur?>()

            for (detail in detailList) {
                val pengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detail.idDetail)
                pengukuranMap[detail.posisiBan] = pengukuran

                if (pengukuran != null) {
                    Log.d(TAG, "  ✅ Pengukuran ${detail.posisiBan}: alur1=${pengukuran.alur1}, alur2=${pengukuran.alur2}, alur3=${pengukuran.alur3}, alur4=${pengukuran.alur4}")
                } else {
                    Log.w(TAG, "  ⚠️ Pengukuran ${detail.posisiBan}: NULL")
                }
            }

            // 6. Helper function untuk convert PengukuranAlur -> AlurBan
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

            // 7. Build CheckDetail dengan mapping yang BENAR
            val checkDetail = CheckDetail(
                idCek = check.idPengecekan,
                tanggalCek = check.tanggalMs,
                tanggalReadable = DateUtils.formatDate(check.tanggalMs),
                waktuReadable = DateUtils.formatTime(check.waktuMs),
                namaBus = bus.namaBus,
                platNomor = bus.platNomor,

                // ✅ Status mapping (dari DetailBan atau fallback ke Pengecekan)
                statusDka = detailMap["DKA"]?.status ?: check.statusDka,
                statusDki = detailMap["DKI"]?.status ?: check.statusDki,
                statusBka = detailMap["BKA"]?.status ?: check.statusBka,
                statusBki = detailMap["BKI"]?.status ?: check.statusBki,

                // ✅ Alur mapping
                alurDka = createAlurBan(pengukuranMap["DKA"]),
                alurDki = createAlurBan(pengukuranMap["DKI"]),
                alurBka = createAlurBan(pengukuranMap["BKA"]),
                alurBki = createAlurBan(pengukuranMap["BKI"])
            )

            Log.d(TAG, "✅ CheckDetail berhasil dibuat:")
            Log.d(TAG, "   - DKA: status=${checkDetail.statusDka}, alur=${checkDetail.alurDka}")
            Log.d(TAG, "   - DKI: status=${checkDetail.statusDki}, alur=${checkDetail.alurDki}")
            Log.d(TAG, "   - BKA: status=${checkDetail.statusBka}, alur=${checkDetail.alurBka}")
            Log.d(TAG, "   - BKI: status=${checkDetail.statusBki}, alur=${checkDetail.alurBki}")
            Log.d(TAG, "========== END getCheckDetail ==========")

            return@withContext checkDetail

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR di getCheckDetail: ${e.message}", e)
            return@withContext null
        }
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

    suspend fun completeCheck(idCek: Long) {
        val detailList = detailBanDao.getDetailsByCheckId(idCek)
        for (detail in detailList) {
            if (detail.status == null) {
                val pengukuran = pengukuranAlurDao.getPengukuranByDetailBanId(detail.idDetail)
                if (pengukuran != null) {
                    val alurList = listOfNotNull(
                        pengukuran.alur1,
                        pengukuran.alur2,
                        pengukuran.alur3,
                        pengukuran.alur4
                    )
                    if (alurList.isNotEmpty()) {
                        val minAlur = alurList.minOrNull() ?: 0f
                        val isAus = minAlur < 1.6f
                        detailBanDao.updateDetailBan(detail.copy(status = isAus))
                    }
                }
            }
        }
    }

    suspend fun getPengukuranMapByPengecekanIdsForExport(
        pengecekanIds: List<Long>
    ): Map<Long, List<DownloadHelper.PengukuranWithPosisi>> {
        if (pengecekanIds.isEmpty()) return emptyMap()

        val pengukuranEntities = pengecekanDao.getPengukuranByPengecekanIds(pengecekanIds)

        return pengukuranEntities.groupBy { it.pengecekanId }
            .mapValues { entry ->
                entry.value.map { entity ->
                    DownloadHelper.PengukuranWithPosisi(
                        posisiBan = entity.posisiBan,
                        pengukuran = PengukuranAlur(
                            idPengukuranAlur = entity.idPengukuranAlur,
                            detailBanId = entity.detailBanId,
                            alur1 = entity.alur1,
                            alur2 = entity.alur2,
                            alur3 = entity.alur3,
                            alur4 = entity.alur4
                        )
                    )
                }
            }
    }

    private fun computeSummaryStatus(
        dka: Boolean?,
        dki: Boolean?,
        bka: Boolean?,
        bki: Boolean?
    ): String {
        val list = listOf(dka, dki, bka, bki)

        if (list.any { it == null }) {
            return "Belum Selesai"
        }

        if (list.any { it == true }) {
            return "Aus"
        }

        return "Tidak Aus"
    }
}