package com.example.tetires.data

import com.example.tetires.data.local.entity.PengecekanWithBus
import com.example.tetires.data.model.PengecekanRingkas
import java.text.SimpleDateFormat
import java.util.*

fun PengecekanWithBus.toPengecekanRingkas(): PengecekanRingkas {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val readableDate = dateFormat.format(Date(tanggalMs))
    val dka = statusDka == true
    val dki = statusDki == true
    val bka = statusBka == true
    val bki = statusBki == true
    return PengecekanRingkas(
        idCek = idPengecekan,
        tanggalCek = tanggalMs,
        tanggalReadable = readableDate,
        namaBus = namaBus,
        platNomor = platNomor,
        statusDka = dka,
        statusDki = dki,
        statusBka = bka,
        statusBki = bki,
        summaryStatus = if (dka || dki || bka || bki) "Aus" else "Tidak Aus"
    )
}
