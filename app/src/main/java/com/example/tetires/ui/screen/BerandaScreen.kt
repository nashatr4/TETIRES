package com.example.tetires.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.room.util.TableInfo
import com.example.tetires.R
import com.example.tetires.data.model.LogItem
import com.example.tetires.ui.component.SearchBar
import com.example.tetires.ui.viewmodel.MainViewModel

// Warna konstanta
private val ColorNavy = Color(0xFF3949A3)
private val ColorBlue = Color(0xFF19A7CE)
private val ColorLightBlue = Color(0xFFAFD3E2)
private val ColorRed = Color(0xFFE33629)
private val ColorGreen = Color(0xFF0C5900)

// --- BerandaScreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaScreen(navController: NavController, viewModel: MainViewModel) {
    val logs by viewModel.recentLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterBy by remember { mutableStateOf("Semua") }

    val filteredLogs = remember(logs, searchQuery, filterBy) {
        if (searchQuery.isBlank()) logs
        else logs.filter { log ->
            when (filterBy) {
                "Tanggal" -> log.tanggalReadable.contains(searchQuery, ignoreCase = true)
                "Plat Nomor" -> log.platNomor.contains(searchQuery, ignoreCase = true)
                "Perusahaan Bus" -> log.namaBus.contains(searchQuery, ignoreCase = true)
                "Status" -> log.summaryStatus.contains(searchQuery, ignoreCase = true)
                else -> log.tanggalReadable.contains(searchQuery, ignoreCase = true)
                        || log.platNomor.contains(searchQuery, ignoreCase = true)
                        || log.namaBus.contains(searchQuery, ignoreCase = true)
                        || log.summaryStatus.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar() },
        containerColor = ColorLightBlue
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ColorLightBlue),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HeroBanner(navController)
                Spacer(modifier = Modifier.height(24.dp))
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    filterBy = filterBy,
                    onSearchChange = {
                        searchQuery = it
                        viewModel.searchLogs(it)
                    },
                    onFilterChange = { filterBy = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 🔥 Scrollable table (langsung di LazyColumn)
            item {
                TableHeader()
            }

            items(filteredLogs) { log ->
                HistoryRow(item = log, navController = navController)
            }
        }
    }
}

@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorNavy, shape = RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Text("Tanggal", Modifier.weight(1.3f), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Perusahaan Bus", Modifier.weight(1.3f), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Plat Nomor", Modifier.weight(1.4f), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text("Status", Modifier.weight(0.6f), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// --- Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar() {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tirelogo),
                    contentDescription = "Logo Tetires Hitam",
                    modifier = Modifier.size(55.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TETIRES",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

// --- Hero Banner ---
@Composable
fun HeroBanner(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sadtire),
                contentDescription = "Ilustrasi Ban",
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ayo lakukan pengecekan ban busmu!",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate("list_bus") },
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorNavy),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(text = "Lihat Bus", color = Color.White)
                }
            }
        }
    }
}

// --- Search & Filter ---
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    filterBy: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
                .clip(RoundedCornerShape(50)),
            placeholder = { Text(text = "Pencarian Cepat", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Filter Icon",
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        FilterDropdown(
            currentFilter = filterBy,
            onFilterSelected = onFilterChange
        )
    }
}

@Composable
fun FilterDropdown(
    currentFilter: String,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(52.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.filtericon),
                contentDescription = "Open Filter Menu",
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Tanggal", "PO Bus", "Plat Nomor", "Status").forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- History Section ---
@Composable
fun HistorySection(
    logs: List<LogItem>,
    navController: NavController,
    searchQuery: String,
    filterBy: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(color = ColorLightBlue)
            .padding(horizontal = 16.dp)
            ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchAndFilterSection(
            searchQuery = searchQuery,
            filterBy = filterBy,
            onSearchChange = onSearchChange,
            onFilterChange = onFilterChange
        )
        Spacer(modifier = Modifier.height(24.dp))
        HistoryTable(logs, navController)
    }
}

@Composable
fun HistoryTable(logs: List<LogItem>, navController: NavController) {
    Column {
        // Header tabel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorNavy, shape = RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "Tanggal",
                modifier = Modifier.weight(1.3f),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "PO Bus",
                modifier = Modifier.weight(1.3f),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Plat Nomor",
                modifier = Modifier.weight(1.4f),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Status",
                modifier = Modifier.weight(0.6f),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Ini bagian yang diganti jadi scrollable
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp) // atau .weight(1f) kalau mau fleksibel
        ) {
            items(logs) { log ->
                HistoryRow(item = log, navController = navController)
            }
        }
    }
}


@Composable
fun HistoryRow(item: LogItem, navController: NavController) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
                .clickable {
                    navController.navigate("detailPengecekan/${item.idCek}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.tanggalReadable,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Text(
                text = item.namaBus,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Text(
                text = item.platNomor,
                modifier = Modifier.weight(1.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = item.summaryStatus,
                modifier = Modifier.weight(0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = if (item.summaryStatus == "Aus") ColorRed else ColorGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
    HorizontalDivider(
        thickness = 1.dp,
        color = Color.Black
    )
}


// --- Preview ---
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun BerandaScreenPreview() {
    BerandaPreviewContent()
}

@Composable
private fun BerandaPreviewContent() {
    val previewNav = rememberNavController()

    val dummyLogs = listOf(
        LogItem(1L, System.currentTimeMillis(), "23 April 2025", "22.12", "Sinar Jaya", "AB 1234 GH", "Aus"),
        LogItem(2L, System.currentTimeMillis(), "24 April 2025", "22.12", "Rosalia Indah", "CD 5678 IJ", "Normal"),
        LogItem(3L, System.currentTimeMillis(), "25 April 2025", "22.12", "Haryanto", "EF 9012 KL", "Aus")
    )

    var searchQuery by remember { mutableStateOf("") }
    var filterBy by remember { mutableStateOf("Tanggal") }

    val filteredLogs = remember(searchQuery, filterBy) {
        dummyLogs.filter { log ->
            when (filterBy) {
                "Tanggal" -> log.tanggalReadable.contains(searchQuery, ignoreCase = true)
                "Plat Nomor" -> log.platNomor.contains(searchQuery, ignoreCase = true)
                "PO Bus" -> log.namaBus.contains(searchQuery, ignoreCase = true)
                "Status" -> log.summaryStatus.contains(searchQuery, ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorLightBlue)
        ) {
            item { HeroBanner(previewNav) }

            item {
                HistorySection(
                    logs = filteredLogs,
                    navController = previewNav,
                    searchQuery = searchQuery,
                    filterBy = filterBy,
                    onSearchChange = { newQuery -> searchQuery = newQuery },
                    onFilterChange = { newFilter -> filterBy = newFilter }
                )
            }
        }
    }
}
