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

    /**
     * Select a device to connect to
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
