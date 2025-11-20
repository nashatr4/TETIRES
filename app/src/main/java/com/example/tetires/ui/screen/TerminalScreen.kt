package com.example.tetires.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tetires.ui.viewmodel.AppMode
import com.example.tetires.ui.viewmodel.BluetoothSharedViewModel
import com.example.tetires.ui.viewmodel.BluetoothSharedViewModelFactory
import com.example.tetires.ui.viewmodel.CekBanState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    navController: NavController
) {
    val app = LocalContext.current.applicationContext as Application
    val bluetoothViewModel: BluetoothSharedViewModel = viewModel(
        factory = BluetoothSharedViewModelFactory(app)
    )

    // 🔥 PENTING: Switch ke Terminal Mode saat screen dibuka
    LaunchedEffect(Unit) {
        bluetoothViewModel.switchToTerminalMode()
    }

    val terminalText by bluetoothViewModel.terminalText.collectAsState()
    val isConnected by bluetoothViewModel.isConnected.collectAsState()
    val cekBanState by bluetoothViewModel.cekBanState.collectAsState()
    val dataCount by bluetoothViewModel.dataCount.collectAsState()
    val appMode by bluetoothViewModel.appMode.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Search States
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentResultIndex by remember { mutableStateOf(0) }

    // Auto-scroll ke bawah saat ada data baru
    LaunchedEffect(terminalText) {
        if (scrollState.maxValue > 0) {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bluetooth Terminal", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Mode: ${if (appMode == AppMode.TERMINAL) "FREE (Debug)" else "CEK BAN"}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status bar
            ConnectionStatusCard(
                isConnected = isConnected,
                cekBanState = cekBanState,
                dataCount = dataCount,
                appMode = appMode
            )

            // Connect / Disconnect Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { bluetoothViewModel.connectBluetooth() },
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(50)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFF10B981) else Color(0xFF3949A3)
                    ),
                    enabled = !isConnected
                ) {
                    Text(if (isConnected) "✓ Connected" else "Connect")
                }

                OutlinedButton(
                    onClick = { bluetoothViewModel.disconnectBluetooth() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    enabled = isConnected
                ) {
                    Text("Disconnect")
                }
            }

            // Command buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { bluetoothViewModel.sendCommand("START") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = isConnected
                ) {
                    Text("START", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { bluetoothViewModel.sendCommand("STOP") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = isConnected
                ) {
                    Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(4.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(20.dp),
                    tint = Color.Gray
                )

                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isEmpty()) {
                            searchResults = emptyList()
                            currentResultIndex = 0
                        } else {
                            val results = it.toRegex(RegexOption.IGNORE_CASE)
                                .findAll(terminalText)
                                .map { match -> match.range.first }
                                .toList()
                            searchResults = results
                            currentResultIndex = 0
                        }
                    },
                    placeholder = { Text("Cari log...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent)
                        .padding(horizontal = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    Text(
                        text = "${currentResultIndex + 1}/${searchResults.size}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (searchResults.isNotEmpty()) {
                            currentResultIndex = (currentResultIndex + 1) % searchResults.size
                            coroutineScope.launch {
                                val targetIndex = searchResults[currentResultIndex]
                                scrollState.scrollTo(targetIndex)
                            }
                        }
                    },
                    enabled = searchResults.isNotEmpty()
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Next", tint = Color(0xFF3949A3))
                }
            }

            // Terminal log box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (terminalText.isEmpty()) {
                            "🖥️ Terminal Mode (Free)\n\n" +
                                    "Tips:\n" +
                                    "• Klik Connect untuk mulai\n" +
                                    "• Data TIDAK disimpan ke database\n" +
                                    "• Hanya untuk debugging/testing\n\n" +
                                    "Untuk scan ban lengkap, gunakan CekBan Screen"
                        } else terminalText,
                        color = Color(0xFF00FF88),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Scroll progress indicator
                val scrollProgress = if (scrollState.maxValue > 0) {
                    scrollState.value / scrollState.maxValue.toFloat()
                } else 0f

                // Scrollbar background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )

                // Scroll thumb
                if (scrollState.maxValue > 0) {
                    val thumbHeight = 40.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (scrollProgress * (scrollState.maxValue - thumbHeight.value).coerceAtLeast(0f)).dp)
                            .width(4.dp)
                            .height(thumbHeight)
                            .background(Color(0xFF00FF88), RoundedCornerShape(2.dp))
                    )
                }

                // Clear button
                IconButton(
                    onClick = { bluetoothViewModel.clearTerminal() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0xFFE33629), RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    cekBanState: CekBanState,
    dataCount: Int,
    appMode: AppMode
) {
    Surface(
        color = when {
            isConnected && cekBanState == CekBanState.SCANNING -> Color(0xFFDDEAFE)
            isConnected -> Color(0xFFD9F99D)
            else -> Color(0xFFFECACA)
        },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isConnected) "🟢 Connected to TETIRES" else "🔴 Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (appMode == AppMode.TERMINAL) {
                    Text(
                        text = "Mode: Terminal (data tidak disimpan)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else if (cekBanState != CekBanState.IDLE) {
                    Text(
                        text = when (cekBanState) {
                            CekBanState.SCANNING -> "CekBan: Scanning ($dataCount data)"
                            CekBanState.PROCESSING -> "CekBan: Processing..."
                            CekBanState.RESULT_READY -> "CekBan: Result Ready ✓"
                            CekBanState.SAVED -> "CekBan: Saved ✓"
                            CekBanState.ERROR -> "CekBan: Error ✗"
                            else -> "CekBan: ${cekBanState.name}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Shared connection dengan CekBan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Connection status icon
            if (cekBanState == CekBanState.SCANNING || cekBanState == CekBanState.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF3949A3)
                )
            }
        }
    }
}