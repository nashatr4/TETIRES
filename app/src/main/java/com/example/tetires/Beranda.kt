package com.example.tetires

import android.view.RoundedCorner
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tetires.R
import java.nio.file.WatchEvent

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar()
        Spacer(modifier = Modifier.height(16.dp))
        HeroBanner(navController = navController)
        Spacer(modifier = Modifier.height(16.dp))
//        HistorySection(listHistoryPengecekan = listHistoryPengecekan)
    }
}

@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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

@Composable
fun HeroBanner (navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A90E2))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sadtire),
                contentDescription = "Ilustrasi Ban",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Ayo lakukan pengecekan ban busmu!",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("list_bus") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949A3))
                ) {
                    Text(text = "Lihat Bus", color = Color.White)
                }
            }
        }
    }
}