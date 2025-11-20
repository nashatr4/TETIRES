package com.example.tetires.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * BluetoothHelper - Mengelola koneksi Bluetooth dengan TETIRES/STM32
 *
 * Features:
 * - Auto-connect ke device tertentu (by name/address)
 * - Callback-based untuk data & status
 * - Thread-safe operation
 * - Auto-reconnect (optional)
 */
class BluetoothHelper(private val context: Context) {

    private val TAG = "BluetoothHelper"
    private val dataBuffer = StringBuilder()


    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // Thread untuk membaca data
    private var readThread: Thread? = null
    private var isReading = false

    // Coroutine scope untuk operasi async
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Target device (bisa diset dari luar atau hardcode)
    var targetDeviceName: String = "TETIRES"  // Ganti sesuai nama Bluetooth module
    var targetDeviceAddress: String? = null  // Atau pakai MAC address langsung

    // UUID standar untuk SPP (Serial Port Profile)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Callbacks
    var onDataReceived: ((String) -> Unit)? = null
    var onStatusChange: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth")
            onError?.invoke("Device tidak mendukung Bluetooth")
        } else if (bluetoothAdapter?.isEnabled == false) {
            Log.w(TAG, "Bluetooth is disabled")
            onStatusChange?.invoke("Bluetooth dimatikan. Silakan aktifkan.")
        }
    }

    /**
     * Cek permission Bluetooth (Android 12+)
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Connect ke Bluetooth device
     */
    @SuppressLint("MissingPermission")
    fun connect() {
        if (!hasBluetoothPermission()) {
            onError?.invoke("Permission Bluetooth belum diberikan")
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            onStatusChange?.invoke("Bluetooth dimatikan. Aktifkan dulu!")
            return
        }

        scope.launch {
            try {
                onStatusChange?.invoke("Mencari device...")

                // Cari device berdasarkan name atau address
                val device = findTargetDevice()

                if (device == null) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Device '$targetDeviceName' tidak ditemukan. Pastikan sudah paired.")
                    }
                    return@launch
                }

                onStatusChange?.invoke("Menghubungkan ke ${device.name}...")

                // Buat socket connection
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                // Cancel discovery untuk speed up connection
                bluetoothAdapter?.cancelDiscovery()

                // Connect (blocking call)
                bluetoothSocket?.connect()

                // Dapatkan streams
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                withContext(Dispatchers.Main) {
                    onStatusChange?.invoke("Connected to ${device.name}")
                }

                // Mulai thread untuk baca data
                startReadingData()

            } catch (e: IOException) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke("Koneksi gagal: ${e.message}")
                    disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke("Error: ${e.message}")
                }
            }
        }
    }

    /**
     * Cari device berdasarkan name atau address
     */
    @SuppressLint("MissingPermission")
    private fun findTargetDevice(): BluetoothDevice? {
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: return null

        // Cari berdasarkan address dulu (lebih spesifik)
        if (targetDeviceAddress != null) {
            val device = pairedDevices.find { it.address == targetDeviceAddress }
            if (device != null) return device
        }

        // Kalau tidak ada, cari berdasarkan name
        return pairedDevices.find {
            it.name?.contains(targetDeviceName, ignoreCase = true) == true
        }
    }

    /**
     * Thread untuk membaca data dari Bluetooth
     */
    private fun startReadingData() {
        isReading = true
        readThread = Thread {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (isReading) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val chunk = String(buffer, 0, bytes)
                        dataBuffer.append(chunk)

                        // Pisahkan per baris (\n)
                        var lineEnd = dataBuffer.indexOf("\n")
                        while (lineEnd >= 0) {
                            val line = dataBuffer.substring(0, lineEnd).trim()
                            dataBuffer.delete(0, lineEnd + 1)

                            // Kirim line ke UI
                            scope.launch(Dispatchers.Main) {
                                onDataReceived?.invoke(line)
                            }

                            lineEnd = dataBuffer.indexOf("\n")
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Read error: ${e.message}")
                    isReading = false
                    scope.launch(Dispatchers.Main) {
                        onStatusChange?.invoke("Connection lost")
                        disconnect()
                    }
                    break
                }
            }
        }
        readThread?.start()
    }

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun isPairedDeviceAvailable(): Boolean {
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: return false
        return pairedDevices.any { it.name?.contains(targetDeviceName, ignoreCase = true) == true }
    }

    /**
     * Kirim data (command) ke device
     */
    fun send(data: String) {
        scope.launch {
            try {
                // Tambahkan newline jika belum ada (untuk STM32)
                val dataToSend = if (data.endsWith("\n")) data else "$data\n"

                outputStream?.write(dataToSend.toByteArray())
                outputStream?.flush()

                Log.d(TAG, "Sent: $data")
            } catch (e: IOException) {
                Log.e(TAG, "Send error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke("Gagal kirim: ${e.message}")
                }
            }
        }
    }

    /**
     * Disconnect dari Bluetooth
     */
    fun disconnect() {
        isReading = false

        try {
            readThread?.interrupt()
            readThread = null

            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

            inputStream = null
            outputStream = null
            bluetoothSocket = null

            onStatusChange?.invoke("Disconnected")
            Log.d(TAG, "Disconnected successfully")

        } catch (e: IOException) {
            Log.e(TAG, "Disconnect error: ${e.message}", e)
        }
    }

    /**
     * Cek apakah sedang terkoneksi
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Get list paired devices (untuk UI picker)
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Set target device by name
     */
    fun setTargetDevice(name: String, address: String? = null) {
        targetDeviceName = name
        targetDeviceAddress = address
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Extension function untuk kemudahan
 */
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun BluetoothDevice.getDisplayName(): String {
    return this.name ?: this.address
}