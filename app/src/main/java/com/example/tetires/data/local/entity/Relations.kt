package com.example.tetires.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relasi: DetailBan + PengukuranAlur (1:1)
 */
data class DetailBanWithPengukuranAlur(
    @Embedded val detailBan: DetailBan,

    @Relation(
        parentColumn = "idDetail",
        entityColumn = "detailBanId"
    )
    val pengukuran: PengukuranAlur?
)

/**
 * Relasi: Pengecekan + List<DetailBan> + List<PengukuranAlur> (1:4:4)
 */
data class PengecekanWithDetails(
    @Embedded val pengecekan: Pengecekan,

    @Relation(
        entity = DetailBan::class,
        parentColumn = "idPengecekan",
        entityColumn = "pengecekanId"
    )
    val detailBanList: List<DetailBanWithPengukuranAlur>
)