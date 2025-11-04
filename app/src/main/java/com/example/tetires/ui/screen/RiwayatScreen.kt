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
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.model.PengecekanRingkas
import com.example.tetires.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// ========================== SCREEN ==========================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(
    navController: NavController,
    viewModel: MainViewModel,
    busId: Long?
) {
    // Ambil data bus dan pengecekan
    val busData by produceState<Bus?>(initialValue = null, busId) {
        value = busId?.let { viewModel.getBusById(it) }
    }

    LaunchedEffect(busId) {
        viewModel.loadLast10Checks(busId)
    }

    val items by viewModel.currentBusChecks.collectAsState()

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
                .background(Color.White)
        ) {
            HeaderDeskripsiBus(
                busData = busData,
                onCekBanClick = {
                    busId?.let { viewModel.startCheck(it) }
                }
            )
            Spacer(modifier = Modifier.height(15.dp))
            RiwayatSection(items, navController, viewModel, busId)
        }
    }

    // Observer untuk navigasi ke cek ban
    val startCheckEvent by viewModel.startCheckEvent.observeAsState()
    LaunchedEffect(startCheckEvent) {
        startCheckEvent?.getContentIfNotHandled()?.let { checkId ->
            navController.navigate("cekBan/${busId}/${checkId}")
        }
    }
}

// ========================== HEADER ==========================
@Composable
fun HeaderDeskripsiBus(
    busData: Bus?,
    onCekBanClick: () -> Unit
) {
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = busData?.namaBus ?: "Loading...",
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = busData?.platNomor ?: "-",
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
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCekBanClick,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3)),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Text("Cek Ban")
                }
            }
        }
    }
}

// ========================== LIST ITEM ==========================
@Composable
fun RiwayatListItem(
    item: PengecekanRingkas,
    navController: NavController,
    viewModel: MainViewModel,
    busId: Long?
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
        border = BorderStroke(1.dp, Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.tanggalReadable,
                modifier = Modifier.weight(2f),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.weight(0.2f))

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp),
                color = Color.Black,
                thickness = 1.dp
            )

            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                StatusDot(isAus = item.statusDki == true)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                StatusDot(isAus = item.statusDka == true)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                StatusDot(isAus = item.statusBki == true)
            }
            Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                StatusDot(isAus = item.statusBka == true)
            }

            Box (modifier = Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            expanded = false
                            busId?.let { navController.navigate("cekBan/$it/${item.idCek}") }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            expanded = false
                            showDeleteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lihat Detail") },
                        onClick = {
                            expanded = false
                            navController.navigate("detailPengecekan/${item.idCek}")
                        }
                    )
                }
            }
        }
    }

    // Dialog konfirmasi hapus
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Konfirmasi Hapus") },
            text = { Text("Apakah Anda yakin ingin menghapus pengecekan ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        busId?.let { viewModel.deletePengecekan(item.idCek, it) }
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ========================== SECTION ==========================
@Composable
fun RiwayatSection(
    items: List<PengecekanRingkas>,
    navController: NavController,
    viewModel: MainViewModel,
    busId: Long?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            "Riwayat Pemeriksaan",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Tanggal",
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Left,
                color = Color.Black,
                fontSize = 13.sp
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
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
//                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat pengecekan",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp)
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    RiwayatListItem(item, navController, viewModel, busId)
//                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

    }
}

// ========================== DOT ==========================
@Composable
fun StatusDot(isAus: Boolean) {
    val color = if (isAus) Color(0xFFEF4444) else Color(0xFF10B981)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 1.dp, color = Color.Black, shape = CircleShape)
    )
}

// ========================== PREVIEW ==========================
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewRiwayatScreen() {
    val dummyList = listOf(
        PengecekanRingkas(
            idCek = 1,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "20 Sep 2025",
            waktuReadable = "12.32",
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
            waktuReadable = "12.32",
            namaBus = "Sinar Jaya",
            platNomor = "AB 8874 GH",
            statusDka = false,
            statusDki = false,
            statusBka = false,
            statusBki = false,
            summaryStatus = "Aman"
        )
    )

    val navController = rememberNavController()
    val dummyBus = Bus(namaBus = "Sinar Jaya", platNomor = "AB 8874 GH")

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
            HeaderDeskripsiBus(busData = dummyBus, onCekBanClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(dummyList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
                        border = BorderStroke(1.dp, Color.Black),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.tanggalReadable,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            HorizontalDivider(
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
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}