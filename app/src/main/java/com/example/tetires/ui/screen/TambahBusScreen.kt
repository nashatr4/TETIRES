package com.example.tetires.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tetires.R
import com.example.tetires.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahBusScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var namaPerusahaan by remember { mutableStateOf("") }
    var platNomor by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val busAddedEvent by viewModel.busAddedEvent.observeAsState()

    // Observer untuk navigasi setelah bus ditambahkan
    LaunchedEffect(busAddedEvent) {
        busAddedEvent?.getContentIfNotHandled()?.let {
            navController.navigateUp()
        }
    }

    // Snackbar untuk error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Menambahkan Bus",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF19A7CE),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF19A7CE))
        ) {
            BoxWithConstraints {
                val topPadding = this.maxHeight * 0.25f

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(vertical = 24.dp, horizontal = 40.dp)
                ) {
                    Text(
                        "Perusahaan Bus",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3949A3)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = namaPerusahaan,
                        onValueChange = { namaPerusahaan = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masukkan perusahaan bus") },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Plat Nomor",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3949A3)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = platNomor,
                        onValueChange = { platNomor = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masukkan plat nomor") },
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (namaPerusahaan.isNotBlank() && platNomor.isNotBlank()) {
                                viewModel.addBus(namaPerusahaan, platNomor)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3949A3)
                        ),
                        enabled = !isLoading &&
                                namaPerusahaan.isNotBlank() &&
                                platNomor.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Tambah Bus", fontSize = 16.sp)
                        }
                    }
                }

                // Image Bus di atas
                Image(
                    painter = painterResource(id = R.drawable.menambahbus),
                    contentDescription = "Gambar Bus",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.75f)
                        .fillMaxHeight(0.25f)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}