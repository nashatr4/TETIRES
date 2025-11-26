package com.example.tetires

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chaquo.python.Python
import com.example.tetires.data.local.dao.PengukuranAlurDao
import com.example.tetires.data.local.database.AppDatabase
import com.example.tetires.data.repository.TetiresRepository
import com.example.tetires.ui.screen.*
import com.example.tetires.ui.theme.TETIRESTheme
import com.example.tetires.ui.viewmodel.MainViewModel
import com.example.tetires.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Test Python module (optional)
        testPythonModule()

        // Request Bluetooth permissions (Android 12+)
        requestBluetoothPermissions()

        // Setup database & repository
        val database = AppDatabase.getInstance(this)
        val repository = TetiresRepository(
            busDao = database.busDao(),
            pengecekanDao = database.pengecekanDao(),
            detailBanDao = database.detailBanDao(),
            pengukuranAlurDao = database.pengukuranAlurDao()
        )

        // Jetpack Compose UI
        setContent {
            TETIRESTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(application, repository)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        // Splash Screen
                        composable("splash") {
                            SplashScreen(navController)
                        }

                        // 🏠 Beranda
                        composable("beranda") {
                            BerandaScreen(navController, mainViewModel)
                        }

                        // 💻 Terminal (Debug Mode)
                        composable("terminal") {
                            TerminalScreen(navController)
                        }

                        // 🚌 Daftar Bus
                        composable("list_bus") {
                            DaftarBusScreen(navController, mainViewModel)
                        }

                        // ➕ Tambah Bus
                        composable("tambah_bus") {
                            TambahBusScreen(navController, mainViewModel)
                        }

                        // 📜 Riwayat per Bus
                        composable(
                            route = "riwayat/{busId}",
                            arguments = listOf(navArgument("busId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val busId = backStackEntry.arguments?.getLong("busId") ?: 0L
                            RiwayatScreen(navController, mainViewModel, busId)
                        }

                        // 🔍 Detail Pengecekan
                        // ✅ FIXED: Pakai idCek sebagai parameter
                        composable(
                            route = "detailPengecekan/{idCek}",
                            arguments = listOf(navArgument("idCek") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val idCek = backStackEntry.arguments?.getLong("idCek") ?: 0L
                            DetailPengecekanScreen(navController, mainViewModel, idCek)
                        }

                        // ⚙️ Cek Ban (Guided Mode)
                        // ✅ FIXED: Pakai pengecekanId (karena dari luar, belum tentu ada)
                        composable(
                            route = "cekBan/{busId}/{pengecekanId}",
                            arguments = listOf(
                                navArgument("busId") { type = NavType.LongType },
                                navArgument("pengecekanId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val busId = backStackEntry.arguments?.getLong("busId") ?: 0L
                            val pengecekanId = backStackEntry.arguments?.getLong("pengecekanId") ?: 0L

                            CekBanScreen(
                                navController = navController,
                                mainViewModel = mainViewModel,
                                busId = busId,
                                pengecekanId = pengecekanId
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔬 Test Python module (opsional untuk debugging)
     */
    private fun testPythonModule() {
        try {
            val py = Python.getInstance()
            val module = py.getModule("tire_depth")

            // Test dummy data
            val dummy = arrayListOf(
                "--- SENSOR 1 ---",
                "Pixel[ 280]: 2100.50 mV",
                "Pixel[ 300]: 2150.30 mV",
                "Pixel[ 320]: 2200.10 mV",
            )

            // Panggil fungsi utama
            val result = module.callAttr("predict_file", dummy).toString()

            Log.i("PYTHON_TEST", "✅ Python works:\n$result")

        } catch (e: Exception) {
            Log.e("PYTHON_TEST", "❌ Python error: ${e.message}", e)
        }
    }

    /**
     * 📡 Request Bluetooth permissions untuk Android 12+
     */
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = mutableListOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }

            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("BLUETOOTH_PERM", "Requesting permissions: $missingPermissions")
                ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toTypedArray(),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d("BLUETOOTH_PERM", "✅ All Bluetooth permissions already granted")
            }
        } else {
            Log.d("BLUETOOTH_PERM", "ℹ️ Android < 12, no special Bluetooth permissions required")
        }
    }

    /**
     * 🧾 Handle permission results (Composable version)
     */
    @Composable
    fun RequestBluetoothPermissions() {
        val context = LocalContext.current
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            val allGranted = permissionsResult.values.all { it }
            if (allGranted) {
                Log.i("BLUETOOTH_PERM", "✅ Semua izin Bluetooth diberikan")
            } else {
                Log.w("BLUETOOTH_PERM", "❌ Beberapa izin Bluetooth ditolak")
            }
        }

        LaunchedEffect(Unit) {
            launcher.launch(permissions.toTypedArray())
        }
    }
}