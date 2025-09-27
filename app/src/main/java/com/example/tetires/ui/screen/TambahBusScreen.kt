package com.example.tetires.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tetires.data.local.entity.Bus
import com.example.tetires.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahBusScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var namaBus by remember { mutableStateOf("") }
    var platNomor by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tambah Bus", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = namaBus,
                onValueChange = { namaBus = it },
                label = { Text("Nama Bus") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = platNomor,
                onValueChange = { platNomor = it },
                label = { Text("Plat Nomor") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (namaBus.isNotBlank() && platNomor.isNotBlank()) {
                        viewModel.addBus(namaBus, platNomor)
                        navController.navigateUp()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A5FCD))
            ) {
                Text("Simpan", color = Color.White)
            }
        }
    }
}
