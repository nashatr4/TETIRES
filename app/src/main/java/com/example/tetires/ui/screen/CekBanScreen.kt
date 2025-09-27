package com.example.tetires.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tetires.ui.component.BanButton
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.data.model.PosisiBan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme // Perbaikan: Menambahkan impor untuk MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CekBanScreen(navController: NavController, viewModel: MainViewModel, idCek: Long) {
    // local states for four positions
    var statusDki by remember { mutableStateOf<Boolean?>(null) }
    var statusDka by remember { mutableStateOf<Boolean?>(null) }
    var statusBki by remember { mutableStateOf<Boolean?>(null) }
    var statusBka by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cek Ban", color = MaterialTheme.colorScheme.onPrimary) }, // Perbaikan: Menggunakan skema warna yang sesuai
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onPrimary // Perbaikan: Menggunakan skema warna yang sesuai
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // Perbaikan: Menggunakan skema warna yang sesuai
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {

            Text("Pilih posisi ban lalu tekan untuk toggle Aus/OK. Setelah setiap toggle, data akan disimpan.")
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BanButton(statusAus = statusDki, onClick = {
                    val next = when (statusDki) { null -> false; false -> true; true -> null }
                    statusDki = next
                    // Perbaikan: Menggunakan .name untuk mengambil string dari enum PosisiBan
                    // Juga, perbaiki logika untuk `isAus`
                    val isAus = next == true
                    viewModel.updateCheckPartial(idCek, PosisiBan.DKI.name, 1.5f, isAus)
                })
                BanButton(statusAus = statusDka, onClick = {
                    val next = when (statusDka) { null -> false; false -> true; true -> null }
                    statusDka = next
                    val isAus = next == true
                    viewModel.updateCheckPartial(idCek, PosisiBan.DKA.name, 1.5f, isAus)
                })
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                BanButton(statusAus = statusBki, onClick = {
                    val next = when (statusBki) { null -> false; false -> true; true -> null }
                    statusBki = next
                    val isAus = next == true
                    viewModel.updateCheckPartial(idCek, PosisiBan.BKI.name, 1.5f, isAus)
                })
                BanButton(statusAus = statusBka, onClick = {
                    val next = when (statusBka) { null -> false; false -> true; true -> null }
                    statusBka = next
                    val isAus = next == true
                    viewModel.updateCheckPartial(idCek, PosisiBan.BKA.name, 1.5f, isAus)
                })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                // TODO: Panggil fungsi untuk menyelesaikan pengecekan ban di ViewModel jika semua ban sudah diperiksa
                navController.navigate("detail/$idCek")
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Detail Pengecekan")
            }
        }
    }
}
