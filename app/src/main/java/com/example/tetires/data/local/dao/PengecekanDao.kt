package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.local.entity.PengecekanWithBus
import kotlinx.coroutines.flow.Flow

@Dao
interface PengecekanDao {

    @Query(
        """
        SELECT 
            p.idPengecekan,
            p.tanggalMs,
            p.waktuMs,
            p.statusDka,
            p.statusDki,
            p.statusBka,
            p.statusBki,
            b.namaBus,
            b.platNomor
        FROM pengecekan p
        INNER JOIN bus b ON p.busId = b.idBus
        WHERE p.busId = :busId
        ORDER BY p.tanggalMs DESC
        LIMIT 10
    """
    )
    fun getLast10Checks(busId: Long): Flow<List<PengecekanWithBus>>

    @Query("DELETE FROM pengecekan WHERE idPengecekan = :id")
    suspend fun deletePengecekanById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengecekan(pengecekan: Pengecekan): Long

    @Update
    suspend fun updatePengecekan(pengecekan: Pengecekan)

    @Delete
    suspend fun deletePengecekan(pengecekan: Pengecekan)

    @Query("SELECT * FROM pengecekan ORDER BY tanggalMs DESC")
    fun getAllPengecekan(): Flow<List<Pengecekan>>

    @Query("SELECT * FROM pengecekan WHERE idPengecekan = :id LIMIT 1")
    suspend fun getPengecekanById(id: Long): Pengecekan?

    @Query("SELECT * FROM pengecekan WHERE busId = :busId ORDER BY tanggalMs DESC LIMIT 1")
    suspend fun getLatestPengecekanForBus(busId: Long): Pengecekan?

    @Query(
        """
        SELECT 
            p.idPengecekan,
            p.tanggalMs,
            p.waktuMs,
            p.statusDka,
            p.statusDki,
            p.statusBka,
            p.statusBki,
            b.namaBus,
            b.platNomor
        FROM pengecekan p
        INNER JOIN bus b ON p.busId = b.idBus
        ORDER BY p.tanggalMs DESC
        LIMIT 10
    """
    )
    fun getLast10ChecksAllBus(): Flow<List<PengecekanWithBus>>

    /**
     * ✅ Query yang sudah diperbaiki untuk export CSV
     * Pastikan order posisi ban konsisten
     */
    @Query(
        """
    SELECT 
        db.pengecekanId,
        db.posisiBan,
        pa.idPengukuranAlur,
        pa.detailBanId,
        pa.alur1,
        pa.alur2,
        pa.alur3,
        pa.alur4
    FROM pengukuran_alur pa
    INNER JOIN detail_ban db ON pa.detailBanId = db.idDetail
    WHERE db.pengecekanId IN (:pengecekanIds)
    ORDER BY db.pengecekanId DESC, 
             CASE db.posisiBan
                WHEN 'DKI' THEN 1
                WHEN 'DKA' THEN 2
                WHEN 'BKI' THEN 3
                WHEN 'BKA' THEN 4
                ELSE 5
             END
"""
    )
    suspend fun getPengukuranByPengecekanIds(pengecekanIds: List<Long>): List<PengukuranWithPosisiEntity>

    // Entity untuk query result
    data class PengukuranWithPosisiEntity(
        val pengecekanId: Long,
        val posisiBan: String,
        val idPengukuranAlur: Long,
        val detailBanId: Long,
        val alur1: Float?,
        val alur2: Float?,
        val alur3: Float?,
        val alur4: Float?
    )
}