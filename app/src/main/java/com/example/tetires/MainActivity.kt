package com.example.tetires

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.repository.TetiresRepository
import com.example.tetires.ui.screen.*
import com.example.tetires.ui.theme.TETIRESTheme
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.ui.viewmodel.MainViewModelFactory
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek dan minta izin Bluetooth (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
            }
        }

        val database = AppDatabase.getInstance(this)
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
                    val mainViewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = "beranda"
                    ) {
                        composable("beranda") {
                            BerandaScreen(navController, mainViewModel)
                        }
                        composable("terminal") {
                            TerminalScreen(navController, context = LocalContext.current)
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
                            val busId = backStackEntry.arguments?.getLong("busId") ?: 0L
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
                            val idCek = backStackEntry.arguments?.getLong("idCek") ?: 0L
                            CekBanScreen(navController, mainViewModel, idCek)
                        }
                    }
                }
            }
        }
    }
}
