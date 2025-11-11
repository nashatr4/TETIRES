package com.example.tetires.ui.screen

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout  // ← SATU-SATUNYA import ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tetires.R
import com.example.tetires.data.model.PosisiBan
import com.example.tetires.ui.viewmodel.*
import com.example.tetires.util.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekBanScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    busId: Long,
    pengecekanId: Long
) {
    val context = LocalContext.current.applicationContext as Application
    val bluetoothVM: BluetoothSharedViewModel = viewModel(
        factory = BluetoothSharedViewModelFactory(context)
    )
    val deviceInfo by bluetoothVM.deviceInfo.collectAsState()
    val activeDeviceType by bluetoothVM.activeDeviceType.collectAsState()

    val cekBanState by bluetoothVM.cekBanState.collectAsState()
    val scanResults by bluetoothVM.scanResults.collectAsState()
    val statusMessage by bluetoothVM.statusMessage.collectAsState(initial = null)
    val dataCount by bluetoothVM.dataCount.collectAsState()
    val isConnected by bluetoothVM.isConnected.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            bluetoothVM.clearStatusMessage()
        }
    }

    LaunchedEffect(Unit) {
        bluetoothVM.enterCekBanMode(busId, pengecekanId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cek Ban", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        bluetoothVM.resetCekBanContext()
                        navController.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StateIndicatorCard(
                state = cekBanState,
                isConnected = isConnected,
                dataCount = dataCount,
                deviceInfo = deviceInfo,
                activeDeviceType = activeDeviceType
            )

            Spacer(Modifier.height(16.dp))

            BusLayoutWithResults(
                results = scanResults,
                state = cekBanState,
                onSelectPosition = { posisi ->
                    bluetoothVM.selectPositionToScan(posisi)
                }
            )

            Spacer(Modifier.height(16.dp))

            ActionButtons(
                state = cekBanState,
                onStartScan = { bluetoothVM.startScan() },
                onStop = { bluetoothVM.stopScan() },
                onSaveAll = { bluetoothVM.saveAllResults() },
                onRestart = { bluetoothVM.resetCekBanContext() },
                onComplete = {
                    navController.navigate("detailPengecekan/$pengecekanId") {
                        popUpTo("beranda") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun StateIndicatorCard(
    state: CekBanState,
    isConnected: Boolean,
    dataCount: Int,
    deviceInfo: String,
    activeDeviceType: DeviceType
) {
    val bgColor = when (state) {
        CekBanState.IDLE -> Color(0xFFF3F4F6)
        CekBanState.WAITING_SCAN -> Color(0xFFE0F2FE)
        CekBanState.SCANNING -> Color(0xFFDDEAFE)
        CekBanState.PROCESSING -> Color(0xFFE9D5FF)
        CekBanState.RESULT_READY -> Color(0xFFD1FAE5)
        CekBanState.SAVED -> Color(0xFFBBF7D0)
        CekBanState.ERROR -> Color(0xFFFECACA)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (state) {
                        CekBanState.IDLE -> "Pilih posisi ban"
                        CekBanState.WAITING_SCAN -> "Siap melakukan scan"
                        CekBanState.SCANNING -> "Sedang membaca data sensor"
                        CekBanState.PROCESSING -> "Memproses data..."
                        CekBanState.RESULT_READY -> "Hasil scan tersedia"
                        CekBanState.SAVED -> "Data tersimpan"
                        CekBanState.ERROR -> "Terjadi error"
                    },
                    fontWeight = FontWeight.Bold
                )

                if (isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (activeDeviceType) {
                                DeviceType.USB -> Icons.Default.Usb
                                DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
                                else -> Icons.Default.DeviceUnknown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF10B981)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            deviceInfo,
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Text(
                    text = when (state) {
                        CekBanState.SCANNING -> "$dataCount data diterima"
                        CekBanState.PROCESSING -> "Python sedang mengolah..."
                        CekBanState.SAVED -> "Semua disimpan di database"
                        else -> if (!isConnected) "Bluetooth belum terhubung" else "Sistem siap"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )

            }

            when (state) {
                CekBanState.SCANNING,
                CekBanState.PROCESSING ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF3949A3)
                    )

                CekBanState.RESULT_READY ->
                    Icon(Icons.Default.CheckCircle, contentDescription = "OK", tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))

                CekBanState.ERROR ->
                    Icon(Icons.Default.Cancel, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
                else -> {}
            }
        }
    }
}

@Composable
fun BusLayoutWithResults(
    results: Map<PosisiBan, TireScanResult>,
    state: CekBanState,
    onSelectPosition: (PosisiBan) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .padding(vertical = 8.dp)
    ) {
        val (busImage, iconDKI, iconDKA, iconBKI, iconBKA) = createRefs()

        val topGuide = createGuidelineFromTop(0.15f)
        val bottomGuide = createGuidelineFromBottom(0.15f)
        val startGuide = createGuidelineFromStart(0.1f)
        val endGuide = createGuidelineFromEnd(0.1f)

        Image(
            painter = painterResource(id = R.drawable.buscekban),
            contentDescription = "Bus",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .aspectRatio(0.35f)
                .constrainAs(busImage) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // Depan Kiri (DKI)
        ResultIcon(
            modifier = Modifier.constrainAs(iconDKI) {
                top.linkTo(topGuide, margin = -10.dp)
                start.linkTo(startGuide, margin = -4.dp)
            },
            label = "DKI",
            state = state,
            result = results[PosisiBan.DKI],
            onClick = { onSelectPosition(PosisiBan.DKI) }
        )

        // Depan Kanan (DKA)
        ResultIcon(
            modifier = Modifier.constrainAs(iconDKA) {
                top.linkTo(topGuide, margin = -10.dp)
                end.linkTo(endGuide, margin = -4.dp)
            },
            label = "DKA",
            state = state,
            result = results[PosisiBan.DKA],
            onClick = { onSelectPosition(PosisiBan.DKA) }
        )

        // Belakang Kiri (BKI)
        ResultIcon(
            modifier = Modifier.constrainAs(iconBKI) {
                bottom.linkTo(bottomGuide, margin = -10.dp)
                start.linkTo(startGuide, margin = -4.dp)
            },
            label = "BKI",
            state = state,
            result = results[PosisiBan.BKI],
            onClick = { onSelectPosition(PosisiBan.BKI) }
        )

        // Belakang Kanan (BKA)
        ResultIcon(
            modifier = Modifier.constrainAs(iconBKA) {
                bottom.linkTo(bottomGuide, margin = -10.dp)
                end.linkTo(endGuide, margin = -4.dp)
            },
            label = "BKA",
            state = state,
            result = results[PosisiBan.BKA],
            onClick = { onSelectPosition(PosisiBan.BKA) }
        )
    }
}


@Composable
fun ResultIcon(
    modifier: Modifier,
    label: String,
    state: CekBanState,
    result: TireScanResult?,
    onClick: () -> Unit
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val (icon, color) = when {
            result != null -> {
                if (result.minGroove < 1.6f) {
                    // ❌ Worn tire → red cross
                    Icons.Default.Cancel to Color(0xFFEF4444)
                } else {
                    // ✅ Safe tire → green check
                    Icons.Default.CheckCircle to Color(0xFF10B981)
                }
            }
            state == CekBanState.SCANNING || state == CekBanState.PROCESSING -> {
                Icons.Default.AddCircle to Color.Gray
            }
            else -> {
                Icons.Default.AddCircle to Color.LightGray
            }
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(70.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(64.dp)
            )
        }

        // Show groove depth text with same color logic
        result?.let {
            Text(
                text = "${"%.2f".format(it.minGroove)} mm",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (it.minGroove < 1.6f) Color(0xFFEF4444) else Color(0xFF10B981)
            )
        }
    }
}


@Composable
fun ActionButtons(
    state: CekBanState,
    onStartScan: () -> Unit,
    onStop: () -> Unit,
    onSaveAll: () -> Unit,
    onRestart: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            CekBanState.IDLE -> {
                Text(
                    "👆 Pilih posisi ban untuk mulai scan",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            CekBanState.WAITING_SCAN -> {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Mulai Scan", fontWeight = FontWeight.Bold)
                }
            }

            CekBanState.SCANNING -> {
                Button(
                    onClick = onStop,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Stop & Proses", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text("Atau tunggu auto-stop", fontSize = 12.sp, color = Color.Gray)
            }

            CekBanState.RESULT_READY -> {
                // ✅ Hasil scan ready, user bisa:
                // 1. Pilih posisi lain untuk scan lagi
                // 2. Atau langsung simpan semua

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Hasil tersimpan sementara",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        "Pilih posisi lain atau simpan semua",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onSaveAll,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Simpan Semua Hasil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            CekBanState.SAVED -> {
                // ✅ Data sudah di database
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Data berhasil disimpan!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3))
                ) {
                    Text("Lihat Detail", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Scan Bus Lain")
                }
            }

            CekBanState.ERROR -> {
                // ✅ Error occurred
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Terjadi kesalahan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.size(250.dp, 56.dp),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Coba Lagi", fontWeight = FontWeight.Bold)
                }
            }

            else -> {}
        }
    }
}