package com.example.tetires.data

import com.example.tetires.data.local.entity.PengecekanWithBus
import com.example.tetires.data.model.PengecekanRingkas
import com.example.tetires.util.DateUtils

fun PengecekanWithBus.toPengecekanRingkas(): PengecekanRingkas {
    val summaryStatus = when {
        listOf(statusDka, statusDki, statusBka, statusBki).any { it == null } -> "Belum Selesai"
        listOf(statusDka, statusDki, statusBka, statusBki).any { it == true } -> "Aus"
        else -> "Tidak Aus"
    }

    return PengecekanRingkas(
        idCek = idPengecekan,
        tanggalCek = tanggalMs,
        tanggalReadable = DateUtils.formatDate(tanggalMs),
        waktuReadable = DateUtils.formatTime(tanggalMs),
        namaBus = namaBus,
        platNomor = platNomor,
        statusDka = statusDka,
        statusDki = statusDki,
        statusBka = statusBka,
        statusBki = statusBki,
        summaryStatus = summaryStatus
    )
}
