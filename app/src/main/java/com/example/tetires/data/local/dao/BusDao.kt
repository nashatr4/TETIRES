package com.example.tetires.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tetires.data.local.entity.Bus
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {

    // Insert bus baru (kalau platNomor sama, replace)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBus(bus: Bus): Long

    // Update data bus
    @Update
    suspend fun updateBus(bus: Bus)

    // Hapus bus
    @Delete
    suspend fun deleteBus(bus: Bus)

    // Ambil semua bus (Live/Flow biar auto update di UI)
    @Query("SELECT * FROM bus ORDER BY namaBus ASC")
    fun getAllBus(): Flow<List<Bus>>

    // Cari bus berdasarkan plat nomor
    @Query("SELECT * FROM bus WHERE platNomor = :plat LIMIT 1")
    suspend fun getBusByPlat(plat: String): Bus?

    // Cari bus berdasarkan ID
    @Query("SELECT * FROM bus WHERE idBus = :id LIMIT 1")
    suspend fun getBusById(id: Long): Bus?
}
