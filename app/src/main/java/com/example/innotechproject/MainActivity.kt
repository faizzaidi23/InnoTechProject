package com.example.innotechproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.innotechproject.ui.theme.InnoTechProjectTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var bluetoothHelper: BluetoothHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionHelper = PermissionHelper(this) { granted ->
            if (granted) {
                bluetoothHelper.checkAndRequestEnabled()
            } else {
                // handle denied permissions
            }
        }

        bluetoothHelper = BluetoothHelper(
            activity = this,
            onEnabled = { startBluetoothWork() },
            onUnavailable = { /* handle device without BT */ }
        )

        setContent {
            InnoTechProjectTheme {
                // Your UI will go here
            }
        }

        // Start permission request
        permissionHelper.requestPermissions()
    }

    private fun startBluetoothWork() {
        // Permissions granted and Bluetooth enabled
        // You can start scanning/connecting here
    }
}
