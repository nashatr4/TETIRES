package com.example.tetires

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun MenambahBus(navController: NavController) {
    var namaPerusahaan by remember { mutableStateOf("") }
    var platNomor by remember { mutableStateOf("") }

    Scaffold(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(40.dp)
            ) {
                Text("Perusahaan Bus", fontWeight = FontWeight.SemiBold, color = Color(0xFF3949A3))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = namaPerusahaan,
                    onValueChange = { namaPerusahaan = it },
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = { Text("Masukan perusahaan bus") },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Plat Nomor", fontWeight = FontWeight.SemiBold, color = Color(0xFF3949A3))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = platNomor,
                    onValueChange = { platNomor = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Masukan plat nomor") },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { /* TODO: Menyimpan bus */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3))
                ) {
                    Text("Masuk", fontSize = 16.sp)
                }
            }
            Image(
                painter = painterResource(id = R.drawable.menambahbus),
                contentDescription = "Gambar Bus",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .height(150.dp)
                    .width(200.dp)
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun MenambahBus() {
    MenambahBus(navController = rememberNavController())
}