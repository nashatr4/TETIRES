package com.example.tetires.data.local.dao

import androidx.room.*
import com.example.tetires.data.local.entity.PengukuranAlur
import kotlinx.coroutines.flow.Flow

@Dao
interface PengukuranAlurDao {

    // Insert data pengukuran baru (kalau detailBanId sama, replace)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengukuran(pengukuran: PengukuranAlur): Long

    // Update data pengukuran
    @Update
    suspend fun updatePengukuran(pengukuran: PengukuranAlur)

    // Hapus data pengukuran
    @Delete
    suspend fun deletePengukuran(pengukuran: PengukuranAlur)

    // Ambil semua data pengukuran (bisa dipakai untuk debugging / admin view)
    @Query("SELECT * FROM pengukuran_alur ORDER BY idPengukuranAlur DESC")
    fun getAllPengukuran(): Flow<List<PengukuranAlur>>

    // Ambil pengukuran berdasarkan ID detail ban
    @Query("SELECT * FROM pengukuran_alur WHERE detailBanId = :detailBanId LIMIT 1")
    suspend fun getPengukuranByDetailBanId(detailBanId: Long): PengukuranAlur?

    // Versi Flow agar bisa observe dari UI (misal pakai LiveData atau StateFlow)
    @Query("SELECT * FROM pengukuran_alur WHERE detailBanId = :detailBanId LIMIT 1")
    fun getPengukuranByDetailBanIdFlow(detailBanId: Long): Flow<PengukuranAlur?>

    @Query("""
        SELECT pa.* 
        FROM pengukuran_alur pa
        INNER JOIN detail_ban db ON pa.detailBanId = db.idDetail
        WHERE db.pengecekanId = :pengecekanId
        ORDER BY db.posisiBan
    """)
    fun getPengukuranByPengecekanId(pengecekanId: Long): Flow<List<PengukuranAlur>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPengukuran(pengukuranList: List<PengukuranAlur>): List<Long>
}
