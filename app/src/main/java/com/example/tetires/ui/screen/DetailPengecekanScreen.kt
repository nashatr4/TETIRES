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
    // 🔹 Load data hanya sekali saat idCek berubah
    LaunchedEffect(idCek) {
        viewModel.loadCheckDetail(idCek)
    }

    val detail by viewModel.checkDetail.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    DetailPengecekanContent(detail = d)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Memuat data...")
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
            BanDetailCard("Depan Kiri", detail.statusDki, detail.alurDki, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            BanDetailCard("Depan Kanan", detail.statusDka, detail.alurDka, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Belakang Kiri", detail.statusBki, detail.alurBki, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            BanDetailCard("Belakang Kanan", detail.statusBka, detail.alurBka, Modifier.weight(1f))
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
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Teks posisi di tengah atas
            Text(
                posisi,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth() // Biar benar-benar center
            )

            Spacer(modifier = Modifier.height(5.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF19A7CE))
            Spacer(modifier = Modifier.height(8.dp))

            AlurRow(label = "Alur 1", value = alur?.alur1)
            AlurRow(label = "Alur 2", value = alur?.alur2)
            AlurRow(label = "Alur 3", value = alur?.alur3)
            AlurRow(label = "Alur 4", value = alur?.alur4)

            // 🔹 Bagian isi rata kanan–kiri seperti semula
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Tebal Tapak", color = Color.Gray, fontSize = 12.sp)
//                Text(
//                    "${String.format("%.1f", tebalTapak)} mm",
//                    fontWeight = FontWeight.Medium,
//                    fontSize = 12.sp
//                )
//            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status", color = Color.Gray, fontSize = 12.sp)
                StatusBadge(status)
            }
        }
    }
}

@Composable
private fun AlurRow(label: String, value: Float?) {
    // Tampilkan "N/A" jika data null
    val textValue = value?.let { String.format("%.1f mm", it) } ?: "N/A"

    // Beri warna merah jika < 1.6mm
    val color = when {
        value == null -> Color.Gray
        value < 1.6f -> Color(0xFFEF4444) // Merah
        else -> Color.Black
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(
            textValue,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = color // Warna dinamis
        )
    }
}

@Composable
fun StatusBadge(statusAus: Boolean?) {
    val (text, color) = when (statusAus) {
        true -> "Aus" to Color(0xFFEF4444)
        false -> "Tidak Aus" to Color(0xFF10B981)
        else -> "Tidak Diketahui" to Color.Gray
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Medium)
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
            BanDetailCard("Depan Kiri", detail.statusDki, detail.ukDki, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            BanDetailCard("Depan Kanan", detail.statusDka, detail.ukDka, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BanDetailCard("Belakang Kiri", detail.statusBki, detail.ukBki, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            BanDetailCard("Belakang Kanan", detail.statusBka, detail.ukBka, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

    }
}
