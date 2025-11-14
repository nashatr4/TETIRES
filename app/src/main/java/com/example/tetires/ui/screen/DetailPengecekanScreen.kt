package com.example.tetires.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tetires.data.model.CheckDetail
import com.example.tetires.data.model.AlurBan
import com.example.tetires.ui.viewmodel.MainViewModel

// ---------------- REAL SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPengecekanScreen(
    navController: NavController,
    viewModel: MainViewModel,
    idCek: Long
) {
    LaunchedEffect(idCek) {
        viewModel.loadCheckDetail(idCek)
    }

    val detail by viewModel.checkDetail.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detail Pengecekan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        detail?.let { d ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Bus Card
                BusInfoCard(detail = d)

                // Layout Ban 2x2 dengan 4 alur per ban
                Row(modifier = Modifier.fillMaxWidth()) {
                    BanDetailCard("Depan Kiri", d.statusDki, d.alurDki, Modifier.weight(3f))
                    Spacer(modifier = Modifier.width(10.dp))
                    BanDetailCard("Depan Kanan", d.statusDka, d.alurDka, Modifier.weight(3f))
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    BanDetailCard("Belakang Kiri", d.statusBki, d.alurBki, Modifier.weight(3f))
                    Spacer(modifier = Modifier.width(10.dp))
                    BanDetailCard("Belakang Kanan", d.statusBka, d.alurBka, Modifier.weight(3f))
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun BusInfoCard(detail: CheckDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF2FF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                detail.namaBus,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF1E3A8A)
            )
            Text(
                detail.platNomor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF3949A3)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFB0BEC5))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tanggal", color = Color.Gray, fontSize = 12.sp)
                    Text(detail.tanggalReadable, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Waktu", color = Color.Gray, fontSize = 12.sp)
                    Text(detail.waktuReadable, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}


@Composable
fun DetailPengecekanContent(detail: CheckDetail) {
    Column {
        // 🔹 Info Bus (lebih menonjol)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF2FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    detail.namaBus,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E3A8A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    detail.platNomor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF3949A3),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFFB0BEC5), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tanggal pemeriksaan", color = Color.Gray, fontSize = 13.sp)
                        Text(detail.tanggalReadable, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Waktu pemeriksaan", color = Color.Gray, fontSize = 13.sp)
                        Text(detail.waktuReadable, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 🔹 Layout Ban 2x2
        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Depan Kiri", detail.statusDki, detail.alurDki, Modifier.weight(3f))
            Spacer(modifier = Modifier.width(10.dp))
            BanDetailCard("Depan Kanan", detail.statusDka, detail.alurDka, Modifier.weight(3f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Belakang Kiri", detail.statusBki, detail.alurBki, Modifier.weight(3f))
            Spacer(modifier = Modifier.width(10.dp))
            BanDetailCard("Belakang Kanan", detail.statusBka, detail.alurBka, Modifier.weight(3f))
        }
    }
}

// ---------- Card & Badge ----------
@Composable
fun BanDetailCard(
    posisi: String,
    status: Boolean?,
    alur: AlurBan?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF19A7CE)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header - Posisi Ban
            Text(
                posisi,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF19A7CE))
            Spacer(modifier = Modifier.height(8.dp))

            // Display 4 Alur
            if (alur != null) {
                val alurDisplayList = alur.getFormattedAlurList()

                alurDisplayList.forEach { alurDisplay ->
                    AlurRow(
                        label = alurDisplay.label,
                        value = alurDisplay.value,
                        isWorn = alurDisplay.isWorn,
                        isMissing = alurDisplay.isMissing
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Info Min
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        alur.formattedMinAlur,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if ((alur.minAlur ?: 0f) < 1.6f) Color(0xFFEF4444) else Color(0xFF059669)
                    )
                }
            } else {
                // Belum ada data
                Text(
                    "Belum Diukur",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status", color = Color.Gray, fontSize = 11.sp)
                StatusBadge(status)
            }
        }
    }
}

@Composable
private fun AlurRow(
    label: String,
    value: Float?,
    isWorn: Boolean,
    isMissing: Boolean
) {
    val textValue = value?.let { "%.3f mm".format(it) } ?: "N/A"

    val color = when {
        isMissing -> Color.Gray
        isWorn -> Color(0xFFEF4444) // Merah jika < 1.6mm
        else -> Color(0xFF059669) // Hijau jika >= 1.6mm
    }

    val bgColor = when {
        isMissing -> Color.Transparent
        isWorn -> Color(0xFFEF4444).copy(alpha = 0.3f) // Background merah muda
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.Gray,
            fontSize = 11.sp
        )
        Text(
            textValue,
            fontWeight = if (isWorn) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = color
        )
    }
}

@Composable
fun StatusBadge(statusAus: Boolean?) {
    val (text, color) = when (statusAus) {
        true -> "Aus" to Color(0xFFEF4444)
        false -> "Tidak Aus" to Color(0xFF10B981)
        else -> "Belum Dicek" to Color.Gray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


// ---------------- PREVIEW ----------------
data class DummyCheckDetail(
    val namaBus: String,
    val platNomor: String,
    val tanggalReadable: String,
    val waktuReadable: String,
    val statusDki: Boolean?,
    val statusDka: Boolean?,
    val statusBki: Boolean?,
    val statusBka: Boolean?,
    val ukDki: AlurBan?,
    val ukDka: AlurBan?,
    val ukBki: AlurBan?,
    val ukBka: AlurBan?
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewDetailPengecekanScreen() {
    val alurDki_OK = AlurBan(1.7f, 1.8f, 1.9f, 2.0f)
    val alurDka_AUS = AlurBan(1.4f, 1.6f, 1.7f, 1.5f) // Ada yg < 1.6
    val alurBki_OK = AlurBan(1.8f, 1.8f, 1.9f, 2.1f)
    val alurBka_NULL = null
    val dummyDetail = DummyCheckDetail(
        namaBus = "UGM Trans 01",
        platNomor = "AB 1234 XY",
        tanggalReadable = "22 Okt 2025",
        waktuReadable = "10:30",
        statusDki = false,
        ukDki = alurDki_OK,

        statusDka = true, // Aus
        ukDka = alurDka_AUS,

        statusBki = false,
        ukBki = alurBki_OK,

        statusBka = null, // Belum dicek
        ukBka = alurBka_NULL
    )


    Scaffold {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailPengecekanPreviewUI(detail = dummyDetail, statusMessage = "Update berhasil")
        }
    }
}

@Composable
fun DetailPengecekanPreviewUI(detail: DummyCheckDetail, statusMessage: String?) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF2FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    detail.namaBus,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFF1E3A8A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    detail.platNomor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF3949A3),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tanggal: ${detail.tanggalReadable}", color = Color.Gray, fontSize = 14.sp)
                    Text("Waktu: ${detail.waktuReadable}", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Depan Kiri", detail.statusDki, detail.ukDki, Modifier.weight(3f))
            Spacer(modifier = Modifier.width(16.dp))
            BanDetailCard("Depan Kanan", detail.statusDka, detail.ukDka, Modifier.weight(3f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Belakang Kiri", detail.statusBki, detail.ukBki, Modifier.weight(3f))
            Spacer(modifier = Modifier.width(16.dp))
            BanDetailCard("Belakang Kanan", detail.statusBka, detail.ukBka, Modifier.weight(3f))
        }

        Spacer(modifier = Modifier.height(20.dp))

    }
}
