package com.example.tetires.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothHelper(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var hc05Device: BluetoothDevice? = null
    private var readJob: Job? = null

    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val HC05_NAME = "HC-05"

    var onDataReceived: ((String) -> Unit)? = null
    var onStatusChange: ((String) -> Unit)? = null

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
    }

    fun isSupported(): Boolean = bluetoothAdapter != null
    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true

    @SuppressLint("MissingPermission")
    fun connect() {
        val adapter = bluetoothAdapter ?: return

        if (!adapter.isEnabled) {
            onStatusChange?.invoke("⚠️ Bluetooth belum aktif!")
            return
        }

        // ✅ Cek izin Bluetooth runtime (Android 12+)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onStatusChange?.invoke("⚠️ Akses Bluetooth belum diizinkan oleh user.")
            return
        }

        val pairedDevices = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            onStatusChange?.invoke("❌ Error akses Bluetooth: ${e.message}")
            return
        }

        hc05Device = pairedDevices.firstOrNull { it.name == HC05_NAME }

        if (hc05Device == null) {
            onStatusChange?.invoke("⚠️ Device HC-05 belum di-pair di pengaturan.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                adapter.cancelDiscovery()
                bluetoothSocket = hc05Device!!.createRfcommSocketToServiceRecord(sppUUID)
                bluetoothSocket!!.connect()
                outputStream = bluetoothSocket!!.outputStream
                inputStream = bluetoothSocket!!.inputStream

                withContext(Dispatchers.Main) {
                    onStatusChange?.invoke("🟢 Connected to ${hc05Device!!.name}")
                }

                startReading()
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onStatusChange?.invoke("❌ Gagal konek: ${e.message}")
                }
                disconnect()
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothHelper", "Error closing: ${e.message}")
        }
        onStatusChange?.invoke("🔴 Disconnected")
    }

    fun send(data: String) {
        try {
            outputStream?.write((data + "\n").toByteArray())
            Log.d("BluetoothHelper", "📤 Sent: $data")
        } catch (e: IOException) {
            onStatusChange?.invoke("❌ Error sending: ${e.message}")
        }
    }

    private fun startReading() {
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            try {
                while (isActive && bluetoothSocket?.isConnected == true) {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val data = String(buffer, 0, bytes)
                        withContext(Dispatchers.Main) {
                            onDataReceived?.invoke(data)
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onStatusChange?.invoke("❌ Connection lost: ${e.message}")
                }
                disconnect()
            }
        }
    }
}
