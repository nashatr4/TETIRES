package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.local.entity.PengecekanWithBus
import kotlinx.coroutines.flow.Flow

@Dao
interface PengecekanDao {

    @Query("""
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
    """)
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

    @Query("""
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
    """)
    fun getLast10ChecksAllBus(): Flow<List<PengecekanWithBus>>
}