package com.example.tetires.ui.screen

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R
import com.example.tetires.data.model.PengecekanRingkas
import com.example.tetires.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    navController: NavController,
    viewModel: MainViewModel,
    busId: Long?
) {
    // Load data terakhir
    viewModel.loadLast10Checks(busId)
    val items = viewModel.currentBusChecks.collectAsState().value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Deskripsi Bus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF19A7CE),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFFFFF))
        ) {
            // Header Deskripsi Bus
            HeaderDeskripsiBus()

            Spacer(modifier = Modifier.height(12.dp))

            // Section Riwayat
            RiwayatSection(items)
        }
    }
}

@Composable
fun HeaderDeskripsiBus() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF19A7CE))
            .height(185.dp)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Sinar Jaya", // TODO: bind ke namaBus
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AB 8874 GH", // TODO: bind ke platNomor
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.busicon),
                    contentDescription = "Ilustrasi Bus",
                    modifier = Modifier
                        .height(90.dp)
                        .width(150.dp)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    onClick = { /* TODO: Navigasi ke cek ban */ },
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3)),
                    contentPadding = PaddingValues(horizontal = 60.dp, vertical = 3.dp)
                ) {
                    Text("Cek Ban")
                }
            }
        }
    }
}

@Composable
fun RiwayatSection(list: List<PengecekanRingkas>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        Text("Riwayat Pemeriksaan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Header tabel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tanggal", modifier = Modifier.width(125.dp), textAlign = TextAlign.Left)
            Spacer(modifier = Modifier.width(13.dp))
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Text("D-KI")
            }
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Text("D-KA")
            }
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Text("B-KI")
            }
            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Text("B-KA")
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { item ->
                RiwayatListItem(item)
            }
        }
    }
}

@Composable
fun RiwayatListItem(item: PengecekanRingkas) {
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
            Text(item.tanggalReadable, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)

            Divider(
                modifier = Modifier
                    .height(50.dp)
                    .width(1.dp),
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.width(12.dp))

            StatusDot(item.statusDki == true)
            StatusDot(item.statusDka == true)
            StatusDot(item.statusBki == true)
            StatusDot(item.statusBka == true)

            IconButton(onClick = { /* TODO: detail cek */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
            }
        }
    }
}

@Composable
fun StatusDot(isAus: Boolean) {
    val color = if (isAus) Color(0xFFEF4444) else Color(0xFF10B981)
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 1.dp, color = Color.Black, shape = CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewRiwayatScreen() {
    val dummyList = listOf(
        PengecekanRingkas(
            idCek = 1,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "20 Sep 2025",
            namaBus = "Sinar Jaya",
            platNomor = "AB 8874 GH",
            statusDka = false,
            statusDki = true,
            statusBka = false,
            statusBki = false,
            summaryStatus = "Aus"
        ),
        PengecekanRingkas(
            idCek = 2,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "18 Sep 2025",
            namaBus = "Sinar Jaya",
            platNomor = "AB 8874 GH",
            statusDka = false,
            statusDki = false,
            statusBka = false,
            statusBki = false,
            summaryStatus = "Aman"
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Deskripsi Bus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF19A7CE),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HeaderDeskripsiBus()
            Spacer(modifier = Modifier.height(12.dp))
            RiwayatSection(dummyList)
        }
    }
}
