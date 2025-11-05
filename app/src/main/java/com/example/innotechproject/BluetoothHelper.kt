// kotlin
package com.example.innotechproject

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class BluetoothHelper(
    private val activity: ComponentActivity,
    private val onEnabled: () -> Unit,
    private val onUnavailable: () -> Unit
) {

    private val enableLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) onEnabled()
        else {
            // user declined enabling BT
        }
    }

    fun checkAndRequestEnabled() {
        val btManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter
        if (adapter == null) {
            onUnavailable()
            return
        }

        if (!adapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableLauncher.launch(enableIntent)
        } else {
            onEnabled()
        }
    }
}
