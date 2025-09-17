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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
        RiwayatPemeriksaan("29 April 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AUS),
        RiwayatPemeriksaan("29 Maret 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN),
        RiwayatPemeriksaan("28 Februari 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN),
        RiwayatPemeriksaan("29 Januari 2025", BanStatus.AMAN, BanStatus.AMAN, BanStatus.AMAN, BanStatus.AUS)
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
                .background(Color(0xFF19A7CE))
        ) {
            HeaderDeskripsiBus()
            RiwayatSection(RiwayatPengecekanList = RiwayatPengecekanList)
        }
    }
}

@Composable
fun HeaderDeskripsiBus() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF19A7CE))
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Sinar Jaya", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "AB 8874 GH", color = Color.White, fontSize = 18.sp)
            }
            Image(
                painter = painterResource(id=R.drawable.busicon),
                contentDescription = "Ilustrasi Bus",
                modifier = Modifier.size(100.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* Navigasi ke Cek Ban */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3))
        ) {
            Text("Cek Ban", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun RiwayatSection(RiwayatPengecekanList: List<RiwayatPemeriksaan>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Riwayat Pemeriksaan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        // Header Tabel
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("D-KI", modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = Color.Black)
            Text("D-KA", modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = Color.Black)
            Text("B-KI", modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = Color.Black)
            Text("B-KI", modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = Color.Black)
            Spacer(modifier = Modifier.height(40.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))

    }
}