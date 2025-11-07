package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.Pengecekan
import com.example.tetires.data.local.entity.PengecekanWithBus
import com.example.tetires.data.local.entity.PengecekanByBusWithBus
import com.example.tetires.data.local.entity.PengecekanWithDetails
import com.example.tetires.data.model.LogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PengecekanDao {

    @Query("""
    SELECT 
        p.idPengecekan,
        p.tanggalMs,
        p.tanggalMs AS waktuMs,
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

    // Insert pengecekan baru
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengecekan(pengecekan: Pengecekan): Long

    // Update pengecekan
    @Update
    suspend fun updatePengecekan(pengecekan: Pengecekan)

    // Hapus pengecekan
    @Delete
    suspend fun deletePengecekan(pengecekan: Pengecekan)

    // Ambil semua pengecekan (urut dari terbaru -> lama)
    @Query("SELECT * FROM pengecekan ORDER BY tanggalMs DESC")
    fun getAllPengecekan(): Flow<List<Pengecekan>>

    // Ambil semua pengecekan untuk 1 bus, dengan data bus
    @Query(
        """
        SELECT p.idPengecekan, p.tanggalMs, p.tanggalMs AS waktuMs,
               p.statusDka, p.statusDki, p.statusBka, p.statusBki,
               b.namaBus, b.platNomor
        FROM pengecekan p
        INNER JOIN bus b ON p.busId = b.idBus
        WHERE p.busId = :busId
        ORDER BY p.tanggalMs DESC
        """
    )
    fun getPengecekanByBus(busId: Long?): Flow<List<PengecekanByBusWithBus>>

    // Ambil pengecekan tertentu berdasarkan ID
    @Query("SELECT * FROM pengecekan WHERE idPengecekan = :id LIMIT 1")
    suspend fun getPengecekanById(id: Long): Pengecekan?

    // Cek pengecekan terbaru untuk 1 bus
    @Query("SELECT * FROM pengecekan WHERE busId = :busId ORDER BY tanggalMs DESC LIMIT 1")
    suspend fun getLatestPengecekanForBus(busId: Long): Pengecekan?

    // 🔥 ambil data pengecekan + bus (join)
    @Query("""
    SELECT 
        p.idPengecekan,
        p.tanggalMs,
        p.tanggalMs AS waktuMs,
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

    @Transaction
    @Query("SELECT * FROM pengecekan WHERE idPengecekan = :pengecekanId")
    suspend fun getPengecekanWithDetails(pengecekanId: Long): PengecekanWithDetails?

    @Transaction
    @Query("SELECT * FROM pengecekan WHERE idPengecekan = :pengecekanId")
    fun getPengecekanWithDetailsFlow(pengecekanId: Long): Flow<PengecekanWithDetails?>
}
