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

    // Tebal tapak (mm), null jika belum diukur
    val ukDka: Float? = null,
    val ukDki: Float? = null,
    val ukBka: Float? = null,
    val ukBki: Float? = null,

    // Redundant status fields (nullable) buat memudahkan query / konsistensi UI
    val statusDka: Boolean? = null,
    val statusDki: Boolean? = null,
    val statusBka: Boolean? = null,
    val statusBki: Boolean? = null
)
