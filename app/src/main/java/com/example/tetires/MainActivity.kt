package com.example.tetires

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tetires.Beranda
import com.example.tetires.ui.theme.TETIRESTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TETIRESTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. NavController untuk mengatur navigasi
                    val navController = rememberNavController()

                    // 2. NavHost untuk semua halaman
                    NavHost(
                        navController = navController,
                        startDestination = "beranda" // Beranda sebagai halaman pertama
                    ) {
                        // 3. Daftarkan Halaman Beranda
                        composable(route = "beranda") {
                            Beranda(navController = navController)
                        }
                        composable(route = "list_bus") {
                            ListBusScreen(navController = navController)
                        }

                        composable("riwayat/{busId}") { backStackEntry ->
                            // Bisa pakai busId kalau mau ambil data spesifik bus
                            RiwayatPengecekan(navController = navController)
                        }

                        composable(route = "menambah_bus") {
                            MenambahBus(navController = navController)
                        }

                        composable(route = "cek_ban/{cekId}") { backStackEntry ->
                            CekBan(navController = navController)
                        }

                        composable(route = "detail_pengecekan/{detailId") { backStackEntry ->
                            DetailPengecekan(navController = navController)
                        }
                    }
                }
            }
        }
    }
}