package com.example.tetires.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.tetires.data.model.LogItem
import com.example.tetires.ui.component.SearchBar
import com.example.tetires.ui.viewmodel.MainViewModel

// Warna konstanta
private val ColorNavy = Color(0xFF3949A3)
private val ColorLightBlue = Color(0xFFAFD3E2)
private val ColorRed = Color(0xFFE33629)
private val ColorGreen = Color(0xFF0D5900)
private val ColorGray = Color(0xFFE0E0E0)
private val ColorDarkText = Color(0xFF333333)

@Composable
fun BerandaScreen(navController: NavController, viewModel: MainViewModel) {
    val logs by viewModel.recentLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterBy by remember { mutableStateOf("Tanggal") }

    val filteredLogs = remember(logs, searchQuery, filterBy) {
        logs.filter { log ->
            when (filterBy) {
                "Tanggal" -> log.tanggalReadable.contains(searchQuery, ignoreCase = true)
                "Plat Nomor" -> log.platNomor.contains(searchQuery, ignoreCase = true)
                "Perusahaan Bus" -> log.namaBus.contains(searchQuery, ignoreCase = true)
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
            item {
                HeroBanner(navController)
            }

            item {
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    filterBy = filterBy,
                    onSearchChange = { query ->
                        searchQuery = query
                        viewModel.searchLogs(query)
                    },
                    onFilterChange = { filterBy = it }
                )
            }

            item {
                HistorySection(filteredLogs)
            }
        }
    }
}

@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp),
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
            color = ColorDarkText
        )
    }
}
// Hero Section
@Composable
fun HeroBanner (navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF4A90E2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sadtire),
                contentDescription = "Ilustrasi Ban",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Ayo lakukan pengecekan ban busmu!",
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate("list_bus") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3)),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(text = "Lihat Bus", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    filterBy: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SearchBar(
                hint = "Search cepat...",
                onQueryChanged = { query ->
                    onSearchChange(query)
                }
            )
        }
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
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            Text(
                text = "☰",
                fontSize = 20.sp,
                color = ColorNavy,
                fontWeight = FontWeight.Bold
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Tanggal", "Perusahaan Bus", "Plat Nomor", "Status").forEach { filter ->
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
@Composable
fun HistorySection(logs: List<LogItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorLightBlue),
        horizontalAlignment = Alignment.CenterHorizontally // biar table di tengah
    ) {
        HistoryTable(logs)
    }
}

@Composable
fun HistoryTable(logs: List<LogItem>) {
    Column () {
        Row(
            modifier = Modifier
                .background(Color(0xFF3949A3), shape = RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp)
        ) {

            Text(
                text = "Tanggal",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Perusahaan Bus",
                modifier = Modifier.weight(1.2f),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Plat Nomor",
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Status",
                modifier = Modifier.weight(0.8f),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

        }

        // Rows
        logs.forEach { log ->
            HistoryRow(item = log)
        }


    }
}


@Composable
fun HistoryRow(item: LogItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorLightBlue)
            .padding( vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, ColorGray, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.tanggalReadable,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = ColorDarkText
        )
        Text(
            text = item.namaBus,
            modifier = Modifier.weight(1.2f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = ColorDarkText
        )
        Text(
            text = item.platNomor,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            color = ColorDarkText
        )
        Text(
            text = item.summaryStatus,
            modifier = Modifier.weight(0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = if (item.summaryStatus == "Aus") ColorRed else ColorGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun BerandaScreenPreview() {
    BerandaPreviewContent()
}

@Composable
private fun BerandaPreviewContent() {
    // Dummy data untuk preview
    val dummyLogs = listOf(
        LogItem(
            idCek = 1L,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "23 April 2025",
            namaBus = "Sinar Jaya",
            platNomor = "AB 1234 GH",
            summaryStatus = "Aus"
        ),
        LogItem(
            idCek = 2L,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "24 April 2025",
            namaBus = "Rosalia Indah",
            platNomor = "CD 5678 IJ",
            summaryStatus = "Normal"
        ),
        LogItem(
            idCek = 3L,
            tanggalCek = System.currentTimeMillis(),
            tanggalReadable = "25 April 2025",
            namaBus = "Haryanto",
            platNomor = "EF 9012 KL",
            summaryStatus = "Aus"
        )
    )

    var searchQuery by remember { mutableStateOf("") }
    var filterBy by remember { mutableStateOf("Tanggal") }

    val filteredLogs = remember(searchQuery, filterBy) {
        dummyLogs.filter { log ->
            when (filterBy) {
                "Tanggal" -> log.tanggalReadable.contains(searchQuery, ignoreCase = true)
                "Plat Nomor" -> log.platNomor.contains(searchQuery, ignoreCase = true)
                "Perusahaan Bus" -> log.namaBus.contains(searchQuery, ignoreCase = true)
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
            item {
                HeroBanner(rememberNavController())
            }

            item {
                SearchAndFilterSection(
                    searchQuery = searchQuery,
                    filterBy = filterBy,
                    onSearchChange = { searchQuery = it },
                    onFilterChange = { filterBy = it }
                )
            }

            item {
                HistorySection(filteredLogs)
            }
        }
    }
}