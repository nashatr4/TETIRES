package com.example.tetires

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

enum class DetailBanStatus {
    AMAN,
    AUS
}

data class DetailBan(
    val posisi: String,
    val tebalTapak: String,
    val status: DetailBanStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPengecekan(navController: NavController) {
    val detailPengecekan = listOf(
        DetailBan("Depan Kiri", "1.5 mm", DetailBanStatus.AMAN),
        DetailBan("Depan Kanan", "1.5 mm", DetailBanStatus.AMAN),
        DetailBan("Belakang Kiri", "1.5 mm", DetailBanStatus.AMAN),
        DetailBan("Belakang Kanan", "1.5 mm", DetailBanStatus.AMAN)
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TanggalPemeriksaan(tanggal = "29 Maret 2025")
            Spacer(modifier = Modifier.height(24.dp))
            // Baris Pertama
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DetailBanCard(detail = detailPengecekan[0])
                }
                Box(modifier = Modifier.weight(1f)) {
                    DetailBanCard(detail = detailPengecekan[1])
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Baris Kedua
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DetailBanCard(detail = detailPengecekan[2])
                }
                Box(modifier = Modifier.weight(1f)) {
                    DetailBanCard(detail = detailPengecekan[3])
                }
            }
        }
    }
}

@Composable
fun TanggalPemeriksaan(tanggal: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tanggal pemeriksaan", color = Color.Black, fontSize = 16.sp)
        Text(tanggal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun DetailBanCard(detail: DetailBan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF19A7CE)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(detail.posisi, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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
                Text(detail.tebalTapak, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("status", color = Color.Gray, fontSize = 14.sp)
                StatusBadge(status = detail.status)
            }
        }
    }
}

@Composable
fun StatusBadge(status : DetailBanStatus) {
    val (text, color) = when (status) {
        DetailBanStatus.AMAN -> "ban aman" to Color(0xFF10B981)
        DetailBanStatus.AUS -> "ban aus" to Color(0xFFEF4444)
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

@Preview(showBackground = true)
@Composable
fun DetailPengecekanScreenPreview() {
    DetailPengecekan(navController = rememberNavController())
}