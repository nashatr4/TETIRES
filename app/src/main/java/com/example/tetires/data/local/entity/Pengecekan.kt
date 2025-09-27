package com.example.tetires.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabel pengecekan: menyimpan ringkasan status ke-4 ban untuk suatu pemeriksaan.
 *
 * - idPengecekan: PK auto
 * - busId: FK -> Bus.idBus
 * - tanggalMs: epoch millis (Long)
 * - statusDka / statusDki / statusBka / statusBki: Boolean?
 * null = belum dicek, true = aus, false = tidak aus
 *
 * NOTE: ringkasan status disimpan di sini. Detail ukuran tapak disimpan di DetailBan.
 */
@Entity(
    tableName = "pengecekan",
    foreignKeys = [
        ForeignKey(
            entity = Bus::class,
            parentColumns = ["idBus"],
            childColumns = ["busId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["busId"]), Index(value = ["tanggalMs"])]
)
data class Pengecekan(
    @PrimaryKey(autoGenerate = true)
    val idPengecekan: Long = 0,
    val busId: Long,
    val tanggalMs: Long = System.currentTimeMillis(),

    // status = null -> belum diisi; true = aus; false = tidak aus
    val statusDka: Boolean? = null, // Depan - Kanan (D-KA)
    val statusDki: Boolean? = null, // Depan - Kiri  (D-KI)
    val statusBka: Boolean? = null, // Belakang - Kanan (B-KA)
    val statusBki: Boolean? = null  // Belakang - Kiri  (B-KI)
)
