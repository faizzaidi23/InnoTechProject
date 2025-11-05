package com.example.innotechproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.innotechproject.ui.theme.InnoTechProjectTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var bluetoothHelper: BluetoothHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize permission and bluetooth helpers
        permissionHelper = PermissionHelper(this) { granted ->
            if (granted) {
                bluetoothHelper.checkAndRequestEnabled()
            } else {
                // Permissions denied - app will still show UI but connection will fail
                // You can show a dialog here if needed
            }
        }

        bluetoothHelper = BluetoothHelper(
            activity = this,
            onEnabled = {
                // Bluetooth enabled - UI will be ready to use
            },
            onUnavailable = {
                // Device doesn't support Bluetooth - rare case
            }
        )

        setContent {
            InnoTechProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Show the car control screen
                    CarControlScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }

        // Request permissions when app starts
        permissionHelper.requestPermissions()
    }
}
