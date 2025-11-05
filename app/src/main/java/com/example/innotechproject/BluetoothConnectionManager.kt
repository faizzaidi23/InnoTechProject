package com.example.innotechproject

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth Classic (SPP) connection to ESP32 car
 * Uses RFCOMM socket for serial communication
 */
class BluetoothConnectionManager {

    // Standard SPP UUID for serial communication
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     * Get list of already paired Bluetooth devices
     * User must pair ESP32 in Android settings first
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Connect to a Bluetooth device by MAC address
     * Must be called from a coroutine (background thread)
     */
    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            // Cancel any ongoing discovery to speed up connection
            bluetoothAdapter?.cancelDiscovery()

            // Create RFCOMM socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            // Connect to the device
            bluetoothSocket?.connect()

            // Get output stream for sending commands
            outputStream = bluetoothSocket?.outputStream

            true
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
            false
        } catch (e: SecurityException) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    /**
     * Send a command to the ESP32 car
     * Commands are single characters: F, B, L, R, S, etc.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            outputStream?.write(command.toByteArray())
            outputStream?.flush()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Disconnect and close all streams
     */
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }
}

