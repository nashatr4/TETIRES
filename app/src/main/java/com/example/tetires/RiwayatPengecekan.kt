package com.example.tetires

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.nio.file.WatchEvent

// Data class untuk merepresentasikan satu item bus
data class RiwayatPemeriksaan(
    val id: Int,
    val tanggal: String,
    val statusBanDKI: BanStatus,
    val statusBanDKA: BanStatus,
    val statusBanBKI: BanStatus,
    val statusBanBKA: BanStatus
)

// Enum untuk status ban agar lebih aman dan jelas
enum class BanStatus {
    AMAN,
    AUS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatPengecekan(navController: NavController) {
    val RiwayatPengecekanList = listOf(
        RiwayatPemeriksaan(4, "29 April 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AUS),
        RiwayatPemeriksaan(3, "29 Maret 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN),
        RiwayatPemeriksaan(2, "28 Februari 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN),
        RiwayatPemeriksaan(1, "29 Januari 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AUS)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Deskripsi Bus",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor =  Color(0xFF19A7CE),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFFFFFF))
        ) {
            HeaderDeskripsiBus(navController)
            Spacer(modifier = Modifier.height(12.dp))
            RiwayatSection(RiwayatPengecekanList = RiwayatPengecekanList)
        }
    }
}

@Composable
fun HeaderDeskripsiBus(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF19A7CE))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bagian kiri
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Sinar Jaya",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AB 8874 GH",
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }

            // Bagian kanan
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.busicon),
                    contentDescription = "Ilustrasi Bus",
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Ke Cek Ban */ },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3)),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 24.dp)
                ) {
                    Text("Cek Ban")
                }
            }
        }
    }
}

@Composable
fun RiwayatSection(RiwayatPengecekanList: List<RiwayatPemeriksaan>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Riwayat Pemeriksaan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(10.dp))
        // Header Tabel
        Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tanggal",
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Left
            )

            Spacer(modifier = Modifier.weight(0.2f))

            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                Text("D-KI", color = Color.Black, fontSize = 13.sp)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                Text("D-KA", color = Color.Black, fontSize = 13.sp)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                Text("B-KI", color = Color.Black, fontSize = 13.sp)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                Text("B-KA", color = Color.Black, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(0.8f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(RiwayatPengecekanList) { riwayat ->
                RiwayatListItem(pemeriksaan = riwayat)
            }
        }
    }
}

@Composable
fun RiwayatListItem(pemeriksaan: RiwayatPemeriksaan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
        border = BorderStroke(1.dp, Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pemeriksaan.tanggal, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            HorizontalDivider(
                modifier = Modifier
                    .height(50.dp)
                    .width(1.dp), color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatusDot(status = pemeriksaan.statusBanDKI)
            StatusDot(status = pemeriksaan.statusBanDKA)
            StatusDot(status = pemeriksaan.statusBanBKI)
            StatusDot(status = pemeriksaan.statusBanBKA)
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
            }
        }
    }
}

@Composable
fun StatusDot(status : BanStatus) {
    val color = if (status == BanStatus.AMAN) Color(0xFF10B981) else Color(0xFFEF4444)
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 1.dp, color = Color.Black, shape = CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
fun RiwayatPengecekanPreview() {
    RiwayatPengecekan(navController = rememberNavController())
}