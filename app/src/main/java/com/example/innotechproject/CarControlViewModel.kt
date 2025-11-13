package com.example.innotechproject

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel to manage Bluetooth state and car controls
 * Holds UI state and handles business logic
 */
class CarControlViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothConnectionManager()

    // List of paired Bluetooth devices
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    // Connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Status messages for user feedback
    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Currently selected device
    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    // --- NEW CODE START ---
    // Holds the slider position (0.0f to 1.0f)
    private val _sliderPosition = MutableStateFlow(0.5f) // Default to medium speed
    val sliderPosition: StateFlow<Float> = _sliderPosition.asStateFlow()

    // Holds the last speed character sent
    private var lastSpeedCommand: Char = '5' // Corresponds to default 0.5f
    // --- NEW CODE END ---


    /**
     * Load list of paired devices from Android settings
     */
    fun loadPairedDevices() {
        try {
            val devices = bluetoothManager.getPairedDevices()
            _pairedDevices.value = devices
            _statusMessage.value = if (devices.isEmpty()) {
                "No paired devices. Pair ESP32 in Bluetooth settings first."
            } else {
                "Found ${devices.size} paired device(s)"
            }
        } catch (e: SecurityException) {
            _statusMessage.value = "Permission denied. Please grant Bluetooth permissions."
        }
    }

    /*
        selecting a device to connect to
     */
    fun selectDevice(device: BluetoothDevice) {
        _selectedDevice.value = device
    }

    /**
     * Connect to the selected Bluetooth device
     */
    @Suppress("MissingPermission")
    fun connect() {
        val device = _selectedDevice.value
        if (device == null) {
            _statusMessage.value = "Please select a device first"
            return
        }

        _statusMessage.value = "Connecting to ${device.name ?: device.address}..."

        viewModelScope.launch {
            val success = bluetoothManager.connect(device)
            if (success) {
                _isConnected.value = true
                _statusMessage.value = "Connected to ${device.name ?: device.address}"
                // --- NEW CODE ---
                // Send the default speed setting on connect
                sendSpeedCommand(lastSpeedCommand)
                // --- NEW CODE END ---
            } else {
                _isConnected.value = false
                _statusMessage.value = "Connection failed. Check if device is powered on."
            }
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        _isConnected.value = false
        _statusMessage.value = "Disconnected"
    }

    // --- NEW CODE START ---
    /**
     * Called when the speed slider changes.
     * Maps a float (0.0f - 1.0f) to the 11 speed commands ('0' to 'q')
     */
    fun onSpeedChanged(position: Float) {
        _sliderPosition.value = position

        // Map the 0.0f-1.0f float to an int from 0-10
        val speedStep = (position * 10).toInt()

        val speedChar = when (speedStep) {
            0 -> '0'
            1 -> '1'
            2 -> '2'
            3 -> '3'
            4 -> '4'
            5 -> '5'
            6 -> '6'
            7 -> '7'
            8 -> '8'
            9 -> '9'
            10 -> 'q' // Max speed
            else -> '5' // Default
        }

        // Only send the command if it's different from the last one
        if (speedChar != lastSpeedCommand) {
            lastSpeedCommand = speedChar
            sendSpeedCommand(speedChar)
        }
    }

    /**
     * Private helper to send just a speed command
     */
    private fun sendSpeedCommand(speedChar: Char) {
        if (!_isConnected.value) return // Don't send if not connected

        viewModelScope.launch {
            val success = bluetoothManager.sendCommand(speedChar.toString())
            if (success) {
                _statusMessage.value = "Speed set to $speedChar"
            } else {
                _statusMessage.value = "Failed to send command. Connection lost?"
                _isConnected.value = false
            }
        }
    }
    // --- NEW CODE END ---

    /**
     * Send command to move car forward
     * ESP32 should listen for 'F' character
     */
    fun moveForward() {
        sendCommand("F", "Moving Forward")
    }

    /**
     * Send command to move car backward
     * ESP32 should listen for 'B' character
     */
    fun moveBackward() {
        sendCommand("B", "Moving Backward")
    }

    /**
     * Send command to turn car left
     * ESP32 should listen for 'L' character
     */
    fun turnLeft() {
        sendCommand("L", "Turning Left")
    }

    /**
     * Send command to turn car right
     * ESP32 should listen for 'R' character
     */
    fun turnRight() {
        sendCommand("R", "Turning Right")
    }

    /**
     * Send command to stop the car
     * ESP32 should listen for 'S' character
     */
    fun stop() {
        sendCommand("S", "Stopped")
    }

    /**
     * Set servo motor angle (0-180 degrees)
     * Maps angle to characters 'a' through 'j'
     */
    fun setServoAngle(angle: Float) {
        _sliderPosition.value = angle

        // Map angle (0-180) to characters a-j
        val angleChar = when {
            angle <= 18f -> 'a'  // 0°
            angle <= 36f -> 'b'  // 20°
            angle <= 54f -> 'c'  // 40°
            angle <= 72f -> 'd'  // 60°
            angle <= 90f -> 'e'  // 80°
            angle <= 108f -> 'f' // 100°
            angle <= 126f -> 'g' // 120°
            angle <= 144f -> 'h' // 140°
            angle <= 162f -> 'i' // 160°
            else -> 'j'          // 180°
        }

        sendCommand(angleChar.toString(), "Servo angle: ${angle.toInt()}°")
    }

    /**
     * Helper function to send any command
     */
    private fun sendCommand(command: String, message: String) {
        if (!_isConnected.value) {
            _statusMessage.value = "Not connected to any device"
            return
        }

        viewModelScope.launch {
            val success = bluetoothManager.sendCommand(command)
            if (success) {
                _statusMessage.value = message
            } else {
                _statusMessage.value = "Failed to send command. Connection lost?"
                _isConnected.value = false
            }
        }
    }

    /**
     * Clean up when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}