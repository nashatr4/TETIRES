package com.example.tetires

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R

// Data class untuk merepresentasikan satu item bus
data class Bus(
    val id: Int,
    val nama: String,
    val platNomor: String,
    val tanggalPemeriksaan: String,
    val statusBan: BusStatus
)

// Enum untuk status ban agar lebih aman dan jelas
enum class BusStatus {
    AMAN,
    AUS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListBusScreen(navController: NavController) {
    // Data dummy
    val busList = listOf(
        Bus(1, "Sinar Jaya", "AB 8874 GH", "terakhir diperiksa 28 April 2025", BusStatus.AMAN),
        Bus(2, "Sinar Jaya", "AB 8874 GH", "terakhir diperiksa 28 April 2025", BusStatus.AUS),
        Bus(3, "Sinar Jaya", "AB 8874 GH", "terakhir diperiksa 28 April 2025", BusStatus.AMAN),
        Bus(4, "Sinar Jaya", "AB 8874 GH", "terakhir diperiksa 28 April 2025", BusStatus.AUS),
        Bus(5, "Sinar Jaya", "AB 8874 GH", "terakhir diperiksa 28 April 2025", BusStatus.AMAN),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Daftar Bus",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF19A7CE),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("menambah_bus") },
                shape = CircleShape,
                containerColor = Color(0xFF3A5FCD)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Bus", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BusListHeader()
            LazyColumn(
                contentPadding = PaddingValues(top = 185.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SearchBar()
                }

                items(busList) { bus ->
                    BusListItem(
                        bus = bus,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun BusListHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF19A7CE))
            .padding(bottom = 40.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.busicon),
            contentDescription = "Ilustrasi Bus",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(1.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bus apa yang mau dicek hari ini?",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun BusListItem(bus: Bus, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
        onClick = {
            navController.navigate("riwayat/${bus.id}") // arah ke riwayat
        }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bus.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(bus.platNomor, color = Color.Black, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status = bus.statusBan)
                Spacer(modifier = Modifier.height(12.dp))
                Text(bus.tanggalPemeriksaan, color = Color.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SearchBar() {
    // Dipisahkan karena mungkin akan ada state di sini nanti
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 24.dp),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.filtericon),
                contentDescription = "Filter",
                modifier = Modifier.size(30.dp)
            )
        },
        trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.LightGray
        )
    )
}

@Composable
fun StatusBadge(status: BusStatus) {
    val (text, color) = when (status) {
        BusStatus.AMAN -> "tidak ada ban aus" to Color(0xFF10B981) // Hijau
        BusStatus.AUS -> "1 ban aus" to Color(0xFFEF4444)       // Merah
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(BorderStroke(1.dp, Color.Black), shape = RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun ListBusScreenPreview() {
    ListBusScreen(navController = rememberNavController())
}
