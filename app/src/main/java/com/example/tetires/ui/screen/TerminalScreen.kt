package com.example.tetires.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R
import com.example.tetires.util.BluetoothHelper
import com.example.tetires.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(navController: NavController, context: Context) {
    val viewModel = remember { TerminalViewModel(context) }

    val terminalText by viewModel.terminalText
    val lastCheck by viewModel.lastCheck

    val bluetoothHelper = remember { BluetoothHelper(context) }

    var status by remember { mutableStateOf("🔴 Disconnected") }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 🔍 Search States
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<Int>()) }
    var currentResultIndex by remember { mutableStateOf(0) }

    bluetoothHelper.onDataReceived = {
        viewModel.addLog(it)
    }


    bluetoothHelper.onStatusChange = {
        status = it
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bluetooth Terminal", fontWeight = FontWeight.Bold)  },
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
            // 💡 Status bar
            Surface(
                color = if (status.contains("Connected")) Color(0xFFD9F99D) else Color(0xFFFECACA),
                shape = RoundedCornerShape(40.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(50))
            ) {
                Text(
                    text = "Status: $status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // 🔘 Connect / Disconnect Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { bluetoothHelper.connect() },
                    modifier = Modifier
                        . weight(1f)
                        .shadow(4.dp, RoundedCornerShape(50)),

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Connect")
                }
                OutlinedButton(
                    onClick = { bluetoothHelper.disconnect() },
                    modifier = Modifier
                        . weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text("Disconnect")
                }
            }

            // 🖥️ Terminal Log Header + Search Modern
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.padding(start = 16.dp).size(20.dp),
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
                                val results = "\\b${Regex.escape(it)}\\b".toRegex(RegexOption.IGNORE_CASE)
                                    .findAll(terminalText)
                                    .map { match -> match.range.first }
                                    .toList()
                                searchResults = results
                                currentResultIndex = 0
                            }
                        },
                        placeholder = { Text("Cari log terminal...", fontSize = 14.sp) },
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

                    IconButton(
                        onClick = {
                            if (searchResults.isNotEmpty()) {
                                coroutineScope.launch {
                                    val targetIndex = searchResults.getOrNull(currentResultIndex) ?: 0
                                    scrollState.scrollTo(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color(0xFF3949A3))
                    }
                }
            }

// 🧾 Terminal log box
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
                        text = if (terminalText.isEmpty()) "Belum ada data..." else terminalText,
                        color = Color(0xFF00FF88),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // ⚙️ Scroll progress (satu aja!)
                val scrollProgress = scrollState.value / (scrollState.maxValue.toFloat().coerceAtLeast(1f))

                // Scrollbar background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                )

                // Scroll thumb
                val thumbHeight = 40.dp // tinggi minimal
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (scrollProgress * 200).dp)
                        .width(4.dp)
                        .height(thumbHeight)
                        .background(Color(0xFF00FF88), RoundedCornerShape(2.dp))
                )
                // 🔘 Floating Clear + History Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier
                            .background(Color(0xFFE33629), RoundedCornerShape(50))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Log",
                            tint = Color.White
                        )
                    }

                    if (lastCheck.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.restoreLastLogs() },
                            modifier = Modifier
                                .background(Color(0xFF3949A3), RoundedCornerShape(50))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.history),
                                contentDescription = "History",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TerminalScreenPreview() {
    val fakeNavController = rememberNavController()
    val fakeContext = androidx.compose.ui.platform.LocalContext.current

    TerminalScreen(navController = fakeNavController, context = fakeContext)
}
