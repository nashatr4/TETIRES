package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.DetailBan
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailBanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailBan(detailBan: DetailBan): Long

    @Update
    suspend fun updateDetailBan(detailBan: DetailBan)

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :id")
    suspend fun getDetailsByCheckId(id: Long): List<DetailBan>
    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :pengecekanId")
    fun getDetailsByCheckIdFlow(pengecekanId: Long): Flow<DetailBan?>

    @Query("SELECT * FROM detail_ban WHERE pengecekanId = :id LIMIT 1")
    suspend fun getDetailBanById(id: Long): DetailBan?


    @Delete
    suspend fun deleteDetailBan(detailBan: DetailBan)
}
