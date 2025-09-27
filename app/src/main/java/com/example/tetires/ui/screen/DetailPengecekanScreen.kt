package com.example.tetires.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tetires.ui.viewmodel.MainViewModel
import androidx.compose.runtime.collectAsState
import com.example.tetires.data.model.CheckDetail
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.tetires.ui.component.BanButton
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPengecekanScreen(navController: NavController, viewModel: MainViewModel, idCek: Long) {
    // load detail
    viewModel.loadCheckDetail(idCek)
    val detail = viewModel.checkDetail.collectAsState().value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detail Pengecekan", color = MaterialTheme.colorScheme.onPrimary) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            detail?.let { d ->
                Text(text = "Tanggal: ${d.tanggalReadable}")
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BanDetailCard("D-KI", d.statusDki, d.ukDki.toString())
                    BanDetailCard("D-KA", d.statusDka, d.ukDka.toString())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BanDetailCard("B-KI", d.statusBki, d.ukBki.toString())
                    BanDetailCard("B-KA", d.statusBka, d.ukBka.toString())
                }
            } ?: run {
                Text("Memuat...")
            }
        }
    }
}

@Composable
fun BanDetailCard(posisi: String, status: Boolean?, uk: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(posisi)
            Spacer(modifier = Modifier.height(8.dp))
            BanButton(statusAus = status, onClick = { /* optional edit */ })
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tebal tapak: $uk mm")
        }
    }
}
