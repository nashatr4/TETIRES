package com.example.tetires.util

import android.R.attr.delay
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Priority-based Device Connection Manager
 *
 * Priority Logic:
 * 1. USB (highest priority) - if detected, use USB only
 * 2. Bluetooth (fallback) - if USB not available
 * 3. Auto-reconnect: Bluetooth only (USB must be manual replug)
 *
 * Flow:
 * - Auto-detect on init
 * - Hot-swap detection (USB plug/unplug)
 * - Bluetooth auto-reconnect on disconnect
 */
class DeviceConnectionManager(
    private val context: Application
) {
    private val TAG = "DeviceManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Helpers
    private val bluetoothHelper = BluetoothHelper(context)
    private val usbHelper = UsbSerialHelper(context)

    // Connection states
    private val _activeDevice = MutableStateFlow<DeviceType>(DeviceType.NONE)
    val activeDevice: StateFlow<DeviceType> = _activeDevice.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _deviceInfo = MutableStateFlow("No device connected")
    val deviceInfo: StateFlow<String> = _deviceInfo.asStateFlow()

    // Bluetooth auto-reconnect
    private var bluetoothReconnectAttempts = 0
    private val MAX_RECONNECT_ATTEMPTS = 3
    private var reconnectJob: Job? = null

    // Callbacks
    var onDataReceived: ((String) -> Unit)? = null
    var onStatusChange: ((String) -> Unit)? = null
    var onDebugLog: ((String) -> Unit)? = null

    init {
        setupCallbacks()
        registerUsbReceiver()
        autoDetectDevices()
    }

    /**
     * Setup callbacks untuk kedua device helpers
     */
    private fun setupCallbacks() {
        // Bluetooth callbacks
        bluetoothHelper.onDataReceived = { data ->
            if (_activeDevice.value == DeviceType.BLUETOOTH) {
                debugLog("BT RX: $data")
                onDataReceived?.invoke(data)
            }
        }

        bluetoothHelper.onStatusChange = { status ->
            debugLog("BT Status: $status")

            // Auto-reconnect logic
            if (status.contains("Disconnected", ignoreCase = true)) {
                _isConnected.value = false

                // Hanya auto-reconnect jika:
                // 1. Bluetooth adalah active device
                // 2. USB tidak tersedia
                if (_activeDevice.value == DeviceType.BLUETOOTH && !isUsbAvailable()) {
                    scheduleBluetoothReconnect()
                }
            } else if (status.contains("Connected", ignoreCase = true)) {
                bluetoothReconnectAttempts = 0
                reconnectJob?.cancel()

                if (_activeDevice.value == DeviceType.BLUETOOTH) {
                    _isConnected.value = true
                    _deviceInfo.value = "Bluetooth: TETIRES"
                    onStatusChange?.invoke("Connected to Bluetooth TETIRES")
                }
            }
        }

        // USB callbacks
        usbHelper.onDataReceived = { data ->
            if (_activeDevice.value == DeviceType.USB) {
                debugLog("USB RX: $data")
                onDataReceived?.invoke(data)
            }
        }

        usbHelper.onStatusChange = { status ->
            debugLog("USB Status: $status")

            if (status.contains("Connected", ignoreCase = true)) {
                if (_activeDevice.value == DeviceType.USB) {
                    _isConnected.value = true
                    _deviceInfo.value = usbHelper.getDeviceInfo()
                    onStatusChange?.invoke("Connected to ${usbHelper.getDeviceInfo()}")
                }
            }else if (status.contains("Disconnected", ignoreCase = true)) {
                _isConnected.value = false
                _activeDevice.value = DeviceType.NONE
                debugLog("USB disconnected — switching to Bluetooth")

                scope.launch {
                    delay(500)
                    autoDetectDevices()
                }
            }
        }
    }

    /**
     * Register USB hotplug receiver
     */
    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        context.registerReceiver(usbReceiver, filter)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    debugLog("USB device attached - triggering auto-detect")
                    scope.launch {
                        delay(1000) // Wait for device to be ready
                        autoDetectDevices()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    debugLog("USB device detached")
                    if (_activeDevice.value == DeviceType.USB) {
                        disconnect()
                        scope.launch {
                            delay(500)
                            autoDetectDevices() // Fallback to Bluetooth
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔥 CORE FUNCTION: Auto-detect with Priority System
     *
     * Priority:
     * 1. USB (if available)
     * 2. Bluetooth (if USB not available)
     */
    fun autoDetectDevices() {
        scope.launch {
            debugLog("=== AUTO-DETECT START ===")

            // 1. Check USB first (highest priority)
            val usbAvailable = isUsbAvailable()
            debugLog("USB available: $usbAvailable")

            if (usbAvailable) {
                debugLog("Priority: USB detected → Connecting to USB")

                // Disconnect Bluetooth if connected
                if (_activeDevice.value == DeviceType.BLUETOOTH) {
                    debugLog("Disconnecting Bluetooth (USB has priority)")
                    bluetoothHelper.disconnect()
                }

                connectUsb()
                return@launch
            }

            // 2. Fallback to Bluetooth
            debugLog("Priority: USB not available → Checking Bluetooth")
            val bluetoothAvailable = isBluetoothAvailable()
            debugLog("Bluetooth available: $bluetoothAvailable")

            if (bluetoothAvailable) {
                debugLog("Priority: Bluetooth detected → Connecting to Bluetooth")
                connectBluetooth()
                return@launch
            }

            // 3. No device available
            debugLog("No devices available")
            _activeDevice.value = DeviceType.NONE
            _deviceInfo.value = "No device detected"
            onStatusChange?.invoke("No device available. Please connect USB or Bluetooth.")
        }
    }

    /**
     * Check if USB device is available
     */
    private fun isUsbAvailable(): Boolean {
        return usbHelper.hasUsbDevice()
    }

    /**
     * Check if Bluetooth is available and TETIRES is paired
     */
    private fun isBluetoothAvailable(): Boolean {
        return bluetoothHelper.isBluetoothEnabled() &&
                bluetoothHelper.isPairedDeviceAvailable()
    }

    /**
     * Connect to USB (Priority 1)
     */
    private fun connectUsb() {
        scope.launch {
            try {
                _activeDevice.value = DeviceType.USB
                debugLog("Attempting USB connection...")

                usbHelper.connect()

                // Wait for connection status
                delay(1000)

                if (usbHelper.isConnected()) {
                    _isConnected.value = true
                    _deviceInfo.value = usbHelper.getDeviceInfo()
                    debugLog("✓ USB connected: ${usbHelper.getDeviceInfo()}")
                    onStatusChange?.invoke("Connected to USB: ${usbHelper.getDeviceInfo()}")
                } else {
                    debugLog("✗ USB connection failed")
                    _activeDevice.value = DeviceType.NONE

                    // Fallback to Bluetooth
                    delay(500)
                    autoDetectDevices()
                }
            } catch (e: Exception) {
                debugLog("USB Error: ${e.message}")
                _activeDevice.value = DeviceType.NONE

                // Fallback to Bluetooth
                delay(500)
                autoDetectDevices()
            }
        }
    }

    /**
     * Connect to Bluetooth (Priority 2 / Fallback)
     */
    private fun connectBluetooth() {
        scope.launch {
            try {
                _activeDevice.value = DeviceType.BLUETOOTH
                debugLog("Attempting Bluetooth connection...")

                bluetoothHelper.connect()

                // Connection status akan dihandle di callback
            } catch (e: Exception) {
                debugLog("Bluetooth Error: ${e.message}")
                _activeDevice.value = DeviceType.NONE
                _deviceInfo.value = "Failed to connect any device"
                onStatusChange?.invoke("Connection failed: ${e.message}")
            }
        }
    }

    /**
     * 🔄 Auto-reconnect Bluetooth (infinite until success)
     */
    private fun scheduleBluetoothReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            while (isActive && !bluetoothHelper.isConnected()) {
                attempt++
                val delayMs = 3000L // coba setiap 3 detik
                debugLog("🔁 Bluetooth reconnect attempt $attempt...")
                onStatusChange?.invoke("Reconnecting Bluetooth... (attempt $attempt)")

                connectBluetooth()
                delay(delayMs)
            }

            if (bluetoothHelper.isConnected()) {
                debugLog("✅ Bluetooth reconnected after $attempt attempts")
                onStatusChange?.invoke("Bluetooth reconnected successfully")
            }
        }
    }

    /**
     * Manual connect (untuk Terminal mode)
     */
    fun manualConnect() {
        scope.launch {
            disconnect() // Clear existing connection
            delay(500)
            autoDetectDevices()
        }
    }

    /**
     * Disconnect active device
     */
    fun disconnect() {
        debugLog("Disconnecting active device: ${_activeDevice.value}")

        reconnectJob?.cancel()
        bluetoothReconnectAttempts = 0

        when (_activeDevice.value) {
            DeviceType.BLUETOOTH -> bluetoothHelper.disconnect()
            DeviceType.USB -> usbHelper.disconnect()
            DeviceType.NONE -> {}
            DeviceType.UNKNOWN -> debugLog("Unknown device type, nothing to disconnect")
        }

        _activeDevice.value = DeviceType.NONE
        _isConnected.value = false
        _deviceInfo.value = "Disconnected"
        onStatusChange?.invoke("Disconnected")
    }

    /**
     * Send command to active device
     */
    fun sendCommand(command: String) {
        when (_activeDevice.value) {
            DeviceType.BLUETOOTH -> {
                bluetoothHelper.send(command)
                debugLog("BT TX: $command")
            }
            DeviceType.USB -> {
                usbHelper.send(command)
                debugLog("USB TX: $command")
            }
            DeviceType.NONE -> {
                debugLog("ERROR: No active device to send command")
            }
            else -> { // menangani DeviceType.UNKNOWN atau kemungkinan baru
                debugLog("ERROR: Unknown device type, cannot send command")
            }
        }
    }


    /**
     * Debug logging dengan timestamp
     */
    private fun debugLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        Log.d(TAG, message)
        onDebugLog?.invoke("[$timestamp] $message")
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }

        disconnect()
        scope.cancel()
    }
}

/**
 * Device type enum
 */
enum class DeviceType {
    NONE, BLUETOOTH, USB, UNKNOWN
}

