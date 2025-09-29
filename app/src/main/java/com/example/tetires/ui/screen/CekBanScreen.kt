package com.example.tetires.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tetires.ui.component.BanButton
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.data.model.PosisiBan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekBanScreen(
    navController: NavController,
    viewModel: MainViewModel,
    idCek: Long?
) {
    // Validasi idCek tidak null
    if (idCek == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("Error: ID Pengecekan tidak valid")
        }
        return
    }

    // Local states untuk 4 posisi ban
    var statusDki by remember { mutableStateOf<Boolean?>(null) }
    var statusDka by remember { mutableStateOf<Boolean?>(null) }
    var statusBki by remember { mutableStateOf<Boolean?>(null) }
    var statusBka by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Cek Ban",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Pilih posisi ban lalu tekan untuk toggle Aus/OK. " +
                        "Setelah setiap toggle, data akan disimpan.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Baris pertama: Depan
            Text(
                "DEPAN",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Depan Kiri (DKI)
                BanButton(
                    statusAus = statusDki,
                    onClick = {
                        val next = when (statusDki) {
                            null -> false
                            false -> true
                            true -> null
                        }
                        statusDki = next
                        val isAus = next == true
                        viewModel.updateCheckPartial(
                            idCek = idCek,
                            posisi = PosisiBan.DKI.name,
                            ukuran = 1.5f,
                            isAus = isAus
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // Depan Kanan (DKA)
                BanButton(
                    statusAus = statusDka,
                    onClick = {
                        val next = when (statusDka) {
                            null -> false
                            false -> true
                            true -> null
                        }
                        statusDka = next
                        val isAus = next == true
                        viewModel.updateCheckPartial(
                            idCek = idCek,
                            posisi = PosisiBan.DKA.name,
                            ukuran = 1.5f,
                            isAus = isAus
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Baris kedua: Belakang
            Text(
                "BELAKANG",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Belakang Kiri (BKI)
                BanButton(
                    statusAus = statusBki,
                    onClick = {
                        val next = when (statusBki) {
                            null -> false
                            false -> true
                            true -> null
                        }
                        statusBki = next
                        val isAus = next == true
                        viewModel.updateCheckPartial(
                            idCek = idCek,
                            posisi = PosisiBan.BKI.name,
                            ukuran = 1.5f,
                            isAus = isAus
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // Belakang Kanan (BKA)
                BanButton(
                    statusAus = statusBka,
                    onClick = {
                        val next = when (statusBka) {
                            null -> false
                            false -> true
                            true -> null
                        }
                        statusBka = next
                        val isAus = next == true
                        viewModel.updateCheckPartial(
                            idCek = idCek,
                            posisi = PosisiBan.BKA.name,
                            ukuran = 1.5f,
                            isAus = isAus
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol ke Detail
            Button(
                onClick = {
                    navController.navigate("detailPengecekan/$idCek")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lihat Detail Pengecekan")
            }
        }
    }
}