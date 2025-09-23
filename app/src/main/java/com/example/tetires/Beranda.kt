package com.example.tetires

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

data class PengecekanHistory(
    val tanggal: String,
    val perusahaan: String,
    val platNomor: String,
    val status: String
)

@Composable
fun Beranda(navController: NavController) {
    // Dummy Data
    val listHistoryPengecekan = listOf(
        PengecekanHistory("29 April 2025", "Sinar Jaya", "AB 8874 GH", "Aus"),
        PengecekanHistory("28 April 2025", "Sinar Jaya", "AB 8872 GH", "Normal"),
        PengecekanHistory("26 April 2025", "Sinar Jaya", "AB 8841 GH", "Normal"),
        PengecekanHistory("24 April 2025", "Sinar Jaya", "AB 8862 GH", "Aus"),
        PengecekanHistory("24 April 2025", "Sinar Jaya", "AB 8866 GH", "Normal"),
    )

    Scaffold(
        topBar = { TopAppBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF4A90E2))
        ) {
            HeroBanner(navController = navController)
            HistorySection(listHistoryPengecekan = listHistoryPengecekan)
        }
    }
}

// Top App Section
@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.tirelogo),
            contentDescription = "Logo Tetires Hitam",
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "TETIRES",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

// Hero Section
@Composable
fun HeroBanner (navController: NavController) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4A90E2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sadtire),
                contentDescription = "Ilustrasi Ban",
                modifier = Modifier
                    .fillMaxHeight(0.2f)
                    .fillMaxWidth(0.4f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ayo lakukan pengecekan ban busmu!",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate("list_bus") },
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3)),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(text = "Lihat Bus", color = Color.White)
                }
            }
        }
    }
}

// History Section
@Composable
fun HistorySection(listHistoryPengecekan: List<PengecekanHistory>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFFAFD3E2))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
            // SearchBar
            TextField(
                value = "",
                onValueChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
                    .clip(RoundedCornerShape(30)),
                placeholder = { Text(text = "Cari berdasarkan plat nomor", fontSize = 14.sp) } ,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id=R.drawable.filtericon),
                        contentDescription = "Filter Icon",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Icon(Icons.Default.Search,
                        contentDescription = "Search Icon",
                        modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        Spacer(modifier = Modifier.height(25.dp))
        // History Table
        HistoryTable(listHistoryPengecekan = listHistoryPengecekan)
    }
}

// History Table Section
@Composable
fun HistoryTable(listHistoryPengecekan: List<PengecekanHistory>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3949A3))
                .padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            Text("Tanggal", modifier = Modifier.weight(1f), color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
            Text("Perusahaan Bus", modifier = Modifier.weight(1.5f), color = Color.White, textAlign = TextAlign.Center, maxLines = 1, fontSize = 14.sp)
            Text("Plat Nomor", modifier = Modifier.weight(1.5f), color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
            Text("Status", modifier = Modifier.weight(1f), color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Body (Scrollable)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(listHistoryPengecekan) {
                history -> HistoryRow(history = history)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// History Row
@Composable
fun HistoryRow(history: PengecekanHistory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(history.tanggal, modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(history.perusahaan, modifier = Modifier.weight(1.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(history.platNomor, modifier = Modifier.weight(1.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(
            text = history.status,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = if(history.status == "Normal") Color(0xFF0C5900) else Color(0xFFE33629),
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BerandaScreenPreview() {
    Beranda(navController = rememberNavController())
}