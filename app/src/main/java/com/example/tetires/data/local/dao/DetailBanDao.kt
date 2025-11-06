package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.DetailBan
import com.example.tetires.data.local.entity.DetailBanWithPengukuranAlur
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailBanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailBan(detailBan: DetailBan): Long

    @Update
    suspend fun updateDetailBan(detailBan: DetailBan)

    @Transaction
    @Query("SELECT * FROM detail_ban WHERE idDetail = :detailId LIMIT 1")
    suspend fun getDetailBanWithPengukuran(detailId: Long): DetailBanWithPengukuranAlur?

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :id")
    suspend fun getDetailsByCheckId(id: Long): List<DetailBan>

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :pengecekanId ORDER BY posisiBan")
    fun getDetailsByCheckIdFlow(pengecekanId: Long): Flow<DetailBan?>

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :pengecekanId AND posisiBan = :posisi LIMIT 1")
    suspend fun getDetailByPosisi(pengecekanId: Long, posisi: String): DetailBan?

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :pengecekanId AND posisiBan = :posisi LIMIT 1")
    fun getDetailByPosisiFlow(pengecekanId: Long, posisi: String): Flow<DetailBan?>

    @Delete
    suspend fun deleteDetailBan(detailBan: DetailBan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDetailBan(detailBanList: List<DetailBan>): List<Long>
}
