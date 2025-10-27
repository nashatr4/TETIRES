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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.tetires.R
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.data.model.PengecekanRingkas
import com.example.tetires.ui.viewmodel.MainViewModel

// Status bus
enum class BusStatus { AMAN, AUS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarBusScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val buses by viewModel.buses.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedAusFilter by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daftar Bus", fontWeight = FontWeight.Bold) },
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
                onClick = { navController.navigate("tambah_bus") },
                shape = CircleShape,
                containerColor = Color(0xFF3A5FCD)
            ) { Icon(Icons.Default.Add, contentDescription = "Tambah Bus", tint = Color.White) }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column {
                    BusListHeader(modifier = Modifier.fillMaxWidth())
                    SearchBarWithDropdown(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedAusFilter = selectedAusFilter,
                        onFilterChange = { selectedAusFilter = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Loop semua bus, langsung cek filter
            // Di DaftarBusScreen
            items(buses) { bus ->
                val latestCheck by viewModel.getLastCheckForBus(bus.idBus).collectAsState()
                val ausCount = listOf(
                    latestCheck?.statusDka,
                    latestCheck?.statusDki,
                    latestCheck?.statusBka,
                    latestCheck?.statusBki
                ).count { it == true }

                // --- START: parsing searchQuery untuk ban aus ---
                val searchLower = searchQuery.trim().lowercase()
                val mappedAusFilter = when {
                    searchLower.contains("tidak") || searchLower.contains("0") -> 0
                    searchLower.contains("1") || searchLower.contains("satu") -> 1
                    searchLower.contains("2") || searchLower.contains("dua") -> 2
                    searchLower.contains("3") || searchLower.contains("tiga") -> 3
                    searchLower.contains("4") || searchLower.contains("empat") -> 4
                    else -> null
                }
                // --- END ---

                val matchesSearch = searchQuery.isBlank() ||
                        bus.namaBus.contains(searchQuery, ignoreCase = true) ||
                        bus.platNomor.contains(searchQuery, ignoreCase = true) ||
                        (mappedAusFilter != null && mappedAusFilter == ausCount)

                val matchesAus = selectedAusFilter == null || ausCount == selectedAusFilter

                if (matchesSearch && matchesAus) {
                    BusListItemDatabase(
                        bus = bus,
                        viewModel = viewModel,
                        navController = navController,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithDropdown(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedAusFilter: Int?,
    onFilterChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "Semua" to null,
        "Tidak ada ban aus" to 0,
        "1 ban aus" to 1,
        "2 ban aus" to 2,
        "3 ban aus" to 3,
        "4 ban aus" to 4
    )

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp) // jarak dari header biru
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            placeholder = { if (searchQuery.isEmpty()) Text("Cari bus...", color = Color.Gray) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.Black) },
                    onClick = {
                        onFilterChange(value)
                        expanded = false
                        onSearchChange("") // hapus search saat pilih filter
                    }
                )
            }
        }

        // Icon search di sebelah kanan untuk buka dropdown filter
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Filter",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun BusListHeader(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF19A7CE))
            .padding(bottom = 16.dp, top = 16.dp),
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Bus apa yang mau dicek hari ini?",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun BusListItemDatabase(
    bus: Bus,
    viewModel: MainViewModel,
    navController: NavController,
    modifier: Modifier
) {
    val lastCheckFlow = viewModel.getLastCheckForBus(bus.idBus)
    val latestCheck by lastCheckFlow.collectAsState(initial = null)

    val ausCount = listOf(
        latestCheck?.statusDka,
        latestCheck?.statusDki,
        latestCheck?.statusBka,
        latestCheck?.statusBki
    ).count { it == true }

    val status = if (ausCount > 0) BusStatus.AUS else BusStatus.AMAN
    val statusText = if (ausCount > 0) "$ausCount ban aus" else "tidak ada ban aus"
    val tanggal = latestCheck?.tanggalReadable ?: "Belum pernah diperiksa"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
        onClick = { navController.navigate("riwayat/${bus.idBus}") }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bus.namaBus, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(bus.platNomor, color = Color.Black, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status, statusText)
                Spacer(modifier = Modifier.height(8.dp))
                Text("terakhir diperiksa $tanggal", color = Color.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatusBadge(status: BusStatus, text: String) {
    val color = if (status == BusStatus.AUS) Color(0xFFEF4444) else Color(0xFF10B981)

    Box(
        modifier = Modifier
            .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(50))
            .background(color, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}


@Composable
fun SearchBar(searchQuery: String, onSearchChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        placeholder = {
            Text(
                text = "Masukkan perusahaan bus atau plat bus",
                color = Color.Black,
                fontSize = 14.sp
            )
        },
        shape = RoundedCornerShape(80),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.LightGray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

@Composable
fun BusListItemPreview(bus: Bus, latestCheck: PengecekanRingkas?) {
    val ausCount = listOf(
        latestCheck?.statusDka,
        latestCheck?.statusDki,
        latestCheck?.statusBka,
        latestCheck?.statusBki
    ).count { it == true }

    val status = if (ausCount > 0) BusStatus.AUS else BusStatus.AMAN
    val statusText = if (ausCount > 0) "$ausCount ban aus" else "tidak ada ban aus"
    val tanggal = latestCheck?.tanggalReadable ?: "Belum pernah diperiksa"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAFD3E2)),
        onClick = { }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bus.namaBus, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(bus.platNomor, color = Color.Black, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(status, statusText)
                Spacer(modifier = Modifier.height(12.dp))
                Text("terakhir diperiksa $tanggal", color = Color.Black, fontSize = 10.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BusListPreview() {
    val dummyBuses = listOf(
        Bus(1, "Sinar Jaya", "AB 1234 CD"),
        Bus(2, "Rosalia Indah", "CD 5678 EF"),
        Bus(3, "Haryanto", "B 9876 GH"),
        Bus(4, "Sumber Kencono", "AG 5432 IJ"),
        Bus(5, "Harapan Jaya", "AB 3456 KL")
    )

    val dummyChecks = listOf(
        PengecekanRingkas(1, 0, "28 April 2025", "12.32", "Sinar Jaya", "AB 1234 CD", false, false, false, false, "Aman"),
        PengecekanRingkas(2, 0, "27 April 2025", "12.32", "Rosalia Indah", "CD 5678 EF", true, false, false, false, "Aus"),
        PengecekanRingkas(3, 0, "26 April 2025", "12.32", "Haryanto", "B 9876 GH", true, true, false, false, "Aus"),
        PengecekanRingkas(4, 0, "25 April 2025", "12.32", "Sumber Kencono", "AG 5432 IJ", true, true, true, false, "Aus"),
        PengecekanRingkas(5, 0, "24 April 2025", "12.32", "Harapan Jaya", "AB 3456 KL", true, true, true, true, "Aus")
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dummyBuses.size) { i ->
            BusListItemPreview(dummyBuses[i], dummyChecks[i])
        }
    }
}
