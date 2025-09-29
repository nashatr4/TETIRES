package com.example.tetires

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.repository.TetiresRepository
import com.example.tetires.ui.screen.BerandaScreen
import com.example.tetires.ui.screen.CekBanScreen
import com.example.tetires.ui.screen.DaftarBusScreen
import com.example.tetires.ui.screen.DetailPengecekanScreen
import com.example.tetires.ui.screen.RiwayatScreen
import com.example.tetires.ui.screen.TambahBusScreen
import com.example.tetires.ui.theme.TETIRESTheme
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Inisialisasi Database
        val database = AppDatabase.getInstance(this)

        // 🔹 Buat repository dengan DAO dari database
        val repository = TetiresRepository(
            busDao = database.busDao(),
            pengecekanDao = database.pengecekanDao(),
            detailBanDao = database.detailBanDao()
        )

        setContent {
            TETIRESTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // 🔹 Buat ViewModel dengan Factory
                    val mainViewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository)
                    )

                    // 🔹 Navigation Graph
                    NavHost(
                        navController = navController,
                        startDestination = "beranda"
                    ) {
                        composable("beranda") {
                            BerandaScreen(navController, mainViewModel)
                        }
                        composable("list_bus") {
                            DaftarBusScreen(navController, mainViewModel)
                        }
                        composable("tambah_bus") {
                            TambahBusScreen(navController, mainViewModel)
                        }
                        composable("riwayat/{busId}",
                            arguments = listOf(navArgument("busId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val busId = backStackEntry.arguments?.getLong("busId")
                            RiwayatScreen(navController, mainViewModel, busId)
                        }
                        composable("detailPengecekan/{idCek}",
                            arguments = listOf(navArgument("idCek") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val idCek = backStackEntry.arguments?.getLong("idCek") ?: 0L
                            DetailPengecekanScreen(navController, mainViewModel, idCek)
                        }
                        composable(
                            route = "cekBan/{idCek}",
                            arguments = listOf(navArgument("idCek") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val idCek = backStackEntry.arguments?.getLong("idCek")
                            CekBanScreen(navController, mainViewModel, idCek)
                        }
                    }

                }

            }
        }
    }
}
