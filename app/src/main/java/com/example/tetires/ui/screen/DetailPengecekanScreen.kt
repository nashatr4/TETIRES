package com.example.tetires.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.compose.rememberNavController
import com.example.tetires.data.model.CheckDetail
import com.example.tetires.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPengecekanScreen(
    navController: NavController,
    viewModel: MainViewModel,
    idCek: Long
) {
    // ambil detail dari VM
    viewModel.loadCheckDetail(idCek)
    val detail = viewModel.checkDetail.collectAsState().value

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
            DetailPengecekanContent(
                detail = d,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Memuat...")
            }
        }
    }
}

@Composable
fun DetailPengecekanContent(detail: CheckDetail, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Tanggal pemeriksaan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tanggal pemeriksaan", color = Color.Black, fontSize = 16.sp)
            Text(detail.tanggalReadable, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Waktu pemeriksaan", color = Color.Black, fontSize = 16.sp)
            Text(detail.waktuReadable, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Baris Pertama
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BanDetailCard("Depan Kiri", detail.statusDki, detail.ukDki)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                BanDetailCard("Depan Kanan", detail.statusDka, detail.ukDka)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Baris Kedua
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BanDetailCard("Belakang Kiri", detail.statusBki, detail.ukBki)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                BanDetailCard("Belakang Kanan", detail.statusBka, detail.ukBka)
            }
        }
    }
}

@Composable
fun BanDetailCard(posisi: String, status: Boolean?, tebalTapak: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF19A7CE)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                posisi,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF19A7CE)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("tebal tapak", color = Color.Gray, fontSize = 14.sp)
                Text("$tebalTapak mm", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("status", color = Color.Gray, fontSize = 14.sp)
                StatusBadge(statusAus = status)
            }
        }
    }
}

@Composable
fun StatusBadge(statusAus: Boolean?) {
    val (text, color) = when (statusAus) {
        true -> "aus" to Color(0xFFEF4444)
        false -> "tidak aus" to Color(0xFF10B981)
        null -> "tidak diketahui" to Color.Gray
    }
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 1f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewDetailPengecekanScreen() {
    val dummyDetail = CheckDetail(
        idCek = 1L,
        tanggalCek = 1714492800000L,
        tanggalReadable = "30 April 2024",
        waktuReadable = "12.32",
        namaBus = "Bus Pariwisata",
        platNomor = "AB 1234 CD",
        statusDka = false,
        statusDki = true,
        statusBka = false,
        statusBki = true,
        ukDka = 10.5f,
        ukDki = 12.3f,
        ukBka = 9.7f,
        ukBki = 11.8f
    )

    MaterialTheme {
        DetailPengecekanContent(detail = dummyDetail)
    }
}