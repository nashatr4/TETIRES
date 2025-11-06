package com.example.tetires.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel detail_ban: menyimpan ukuran (tebal tapak) dan (duplikat) status per posisi.
 * One-to-one dengan Pengecekan (satu pengecekan => satu detail_ban).
 *
 * - uk* : Float? (mm), null = belum diukur
 * - status* : Boolean? (nullable sama seperti di Pengecekan) -> redundancy untuk query & konsistensi UI
 */
@Entity(
    tableName = "detail_ban",
    foreignKeys = [
        ForeignKey(
            entity = Pengecekan::class,
            parentColumns = ["idPengecekan"],
            childColumns = ["pengecekanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pengecekanId"], unique = true)]
)
data class DetailBan(
    @PrimaryKey(autoGenerate = true)
    val idDetail: Long = 0,

    // FK ke pengecekan.idPengecekan
    val pengecekanId: Long,
    val posisiBan: String,
    val status: Boolean? = null
)