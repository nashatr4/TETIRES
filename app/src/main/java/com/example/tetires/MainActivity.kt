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
                        // Halaman Beranda
                        composable("beranda") {
                            BerandaScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }

                        // Halaman Daftar Bus
                        composable("list_bus") {
                            DaftarBusScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }

                        // Halaman Tambah Bus
                        composable("tambah_bus") {
                            TambahBusScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }


                        // Halaman Riwayat (dengan parameter busId)
                        composable(
                            route = "riwayat/{busId}",
                            arguments = listOf(
                                navArgument("busId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val busId = backStackEntry.arguments?.getLong("busId")
                            RiwayatScreen(
                                navController = navController,
                                viewModel = mainViewModel,
                                busId = busId
                            )
                            composable("detail_check/{idCek}") { backStackEntry ->
                                val idCek = backStackEntry.arguments?.getString("idCek")?.toLongOrNull()
                                if (idCek != null) {
                                    DetailPengecekanScreen(
                                        navController = navController,
                                        viewModel = mainViewModel,
                                        idCek = idCek
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}
