package com.example.tetires.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import java.io.IOException

/**
 * USB Serial Helper untuk komunikasi dengan STM32 via USB
 *
 * Supported chips:
 * - CH340/CH341
 * - FTDI
 * - CP210x
 * - PL2303
 *
 * Baud rate: 115200 (default untuk STM32)
 */
class UsbSerialHelper(private val context: Context) {

    private val TAG = "UsbSerialHelper"
    private val ACTION_USB_PERMISSION = "com.example.tetires.USB_PERMISSION"

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var usbPort: UsbSerialPort? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    var onDataReceived: ((String) -> Unit)? = null
    var onStatusChange: ((String) -> Unit)? = null

    // Buffer untuk data parsing
    private val dataBuffer = StringBuilder()

    /**
     * Check apakah ada USB device yang tersedia
     */
    fun hasUsbDevice(): Boolean {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        // 🔥 TAMBAHKAN LOGGING DETAIL INI:
        Log.d(TAG, "=== USB DEVICE SCAN ===")
        Log.d(TAG, "Available drivers: ${availableDrivers.size}")

        availableDrivers.forEachIndexed { index, driver ->
            val device = driver.device
            Log.d(TAG, "Driver $index:")
            Log.d(TAG, "  - Device Name: ${device.deviceName}")
            Log.d(TAG, "  - Vendor ID: ${device.vendorId}")
            Log.d(TAG, "  - Product ID: ${device.productId}")
            Log.d(TAG, "  - Manufacturer: ${device.manufacturerName}")
            Log.d(TAG, "  - Product: ${device.productName}")
            Log.d(TAG, "  - Driver: ${driver.javaClass.simpleName}")
        }

        return availableDrivers.isNotEmpty()
    }

    /**
     * Get device info string
     */
    fun getDeviceInfo(): String {
        val driver = getAvailableDriver() ?: return "No USB device"
        val device = driver.device
        return "USB: ${device.manufacturerName ?: "Unknown"} ${device.productName ?: "Serial"}"
    }

    /**
     * Check connection status
     */
    fun isConnected(): Boolean {
        return usbPort != null && usbPort?.isOpen == true
    }

    /**
     * Get available USB driver
     */
    private fun getAvailableDriver(): UsbSerialDriver? {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "No USB devices found")
            return null
        }

        // Prioritas: CH340 > FTDI > CP210x > others
        val priorityOrder = listOf("CH34", "FTDI", "CP210", "PL2303")

        for (priority in priorityOrder) {
            val driver = availableDrivers.find {
                it.device.deviceName.contains(priority, ignoreCase = true) ||
                        it.device.productName?.contains(priority, ignoreCase = true) == true
            }
            if (driver != null) {
                Log.d(TAG, "Found priority device: ${driver.device.productName}")
                return driver
            }
        }

        // Fallback: return first available
        return availableDrivers[0]
    }

    /**
     * Connect ke USB device
     */
    fun connect() {
        scope.launch {
            try {
                val driver = getAvailableDriver()

                if (driver == null) {
                    onStatusChange?.invoke("No USB device found")
                    return@launch
                }

                val device = driver.device

                // Check permission
                if (!usbManager.hasPermission(device)) {
                    Log.d(TAG, "Requesting USB permission...")
                    requestPermission(device)
                    return@launch
                }

                // Open connection
                openConnection(driver)

            } catch (e: Exception) {
                Log.e(TAG, "Connect error: ${e.message}", e)
                onStatusChange?.invoke("USB Error: ${e.message}")
            }
        }
    }

    /**
     * Request USB permission
     */
    private fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbPermissionReceiver, filter)

        usbManager.requestPermission(device, permissionIntent)
        onStatusChange?.invoke("Requesting USB permission...")
    }

    /**
     * USB Permission receiver
     */
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            Log.d(TAG, "USB permission granted")
                            scope.launch {
                                val driver = UsbSerialProber.getDefaultProber().probeDevice(it)
                                if (driver != null) {
                                    openConnection(driver)
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "USB permission denied")
                        onStatusChange?.invoke("USB permission denied")
                    }
                }

                try {
                    context?.unregisterReceiver(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receiver: ${e.message}")
                }
            }
        }
    }

    /**
     * Open USB connection dan mulai reading
     */
    private fun openConnection(driver: UsbSerialDriver) {
        try {
            val connection = usbManager.openDevice(driver.device)

            if (connection == null) {
                onStatusChange?.invoke("Cannot open USB device")
                return
            }

            // Get first port
            val port = driver.ports[0]
            port.open(connection)

            // Configure serial parameters (untuk STM32)
            port.setParameters(
                115200,  // baud rate
                8,       // data bits
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            usbPort = port

            Log.d(TAG, "USB connected: ${driver.device.productName}")
            onStatusChange?.invoke("Connected to USB: ${driver.device.productName}")

            // Start reading data
            startReading()

        } catch (e: IOException) {
            Log.e(TAG, "Error opening USB: ${e.message}", e)
            onStatusChange?.invoke("USB open error: ${e.message}")
        }
    }

    /**
     * Start reading data dari USB
     */
    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(8192)

            while (isActive && usbPort?.isOpen == true) {
                try {
                    val len = usbPort?.read(buffer, 1000) ?: 0

                    if (len > 0) {
                        val data = String(buffer, 0, len)
                        processData(data)
                    }

                } catch (e: IOException) {
                    if (isActive) {
                        Log.e(TAG, "Read error: ${e.message}")
                        onStatusChange?.invoke("USB Disconnected")
                        disconnect()
                    }
                    break
                }
            }
        }
    }

    /**
     * Process incoming data (handle buffer dan parsing)
     */
    private fun processData(data: String) {
        dataBuffer.append(data)

        // Split by newline
        val lines = dataBuffer.toString().split("\n")

        // Process complete lines
        for (i in 0 until lines.size - 1) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                onDataReceived?.invoke(line)
            }
        }

        // Keep last incomplete line in buffer
        dataBuffer.clear()
        dataBuffer.append(lines.last())

        // Prevent buffer overflow
        if (dataBuffer.length > 4096) {
            Log.w(TAG, "Buffer overflow, clearing...")
            dataBuffer.clear()
        }
    }

    /**
     * Send command ke USB device
     */
    fun send(command: String) {
        scope.launch {
            try {
                val data = "$command\n".toByteArray()
                usbPort?.write(data, 1000)
                Log.d(TAG, "Sent: $command")
            } catch (e: IOException) {
                Log.e(TAG, "Send error: ${e.message}")
                onStatusChange?.invoke("Send failed: ${e.message}")
            }
        }
    }

    /**
     * Disconnect USB
     */
    fun disconnect() {
        readJob?.cancel()

        try {
            usbPort?.close()
            usbPort = null
            Log.d(TAG, "USB disconnected")
            onStatusChange?.invoke("USB Disconnected")
        } catch (e: IOException) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }


}