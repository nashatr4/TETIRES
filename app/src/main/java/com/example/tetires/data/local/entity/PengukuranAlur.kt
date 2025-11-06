package com.example.tetires.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel pengukuran_alur:
 * Menyimpan hasil pengukuran 4 alur pada satu ban.
 *
 * - Satu DetailBan memiliki satu PengukuranAlur.
 * - Setiap kolom alur (alur1–alur4) bisa bernilai null jika belum diukur.
 * - Data kedalaman diukur dalam milimeter (mm).
 */
@Entity(
    tableName = "pengukuran_alur",
    foreignKeys = [
        ForeignKey(
            entity = DetailBan::class,
            parentColumns = ["idDetail"],
            childColumns = ["detailBanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["detailBanId"], unique = true)]
)
data class PengukuranAlur(
    @PrimaryKey(autoGenerate = true)
    val idPengukuranAlur: Long = 0,

    // FK ke DetailBan (satu ban punya satu set pengukuran 4 alur)
    val detailBanId: Long,

    // Nilai pengukuran kedalaman per alur (mm)
    val alur1: Float? = null,
    val alur2: Float? = null,
    val alur3: Float? = null,
    val alur4: Float? = null
)
