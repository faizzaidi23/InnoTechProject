package com.example.innotechproject

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Main screen for car control
 * Shows device selection and control buttons
 */
@Composable
fun CarControlScreen(modifier: Modifier = Modifier, viewModel: CarControlViewModel = viewModel()) {

    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    var showDeviceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPairedDevices()
    }

    // Vibrant gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),  // Dark blue-grey
            Color(0xFF203A43),  // Medium blue
            Color(0xFF2C5364)   // Lighter blue
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header
        Text(
            text = "🚗 Car Controller",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Connection Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected)
                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                else
                    Color.White.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device selection button
                Button(
                    onClick = { showDeviceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00ACC1),
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = try { selectedDevice?.name ?: "Select Device" } catch (_: SecurityException) { "Select Device" },
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Connect/Disconnect button
                Button(
                    onClick = {
                        if (isConnected) viewModel.disconnect()
                        else viewModel.connect()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected)
                            Color(0xFFE53935)
                        else
                            Color(0xFF43A047)
                    ),
                    enabled = selectedDevice != null
                ) {
                    Icon(
                        if (isConnected) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Disconnect" else "Connect",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                // Status message
                Spacer(Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = if (isConnected) Color(0xFF66BB6A) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Control Panel (only enabled when connected)
        if (isConnected) {
            ControlPanel(viewModel)
        } else {
            // Show placeholder when not connected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Connect to ESP32 to control the car",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Device selector dialog
    if (showDeviceDialog) {
        DeviceSelectionDialog(
            devices = pairedDevices,
            selectedDevice = selectedDevice,
            onDeviceSelected = { device ->
                viewModel.selectDevice(device)
                showDeviceDialog = false
            },
            onDismiss = { showDeviceDialog = false },
            onRefresh = { viewModel.loadPairedDevices() }
        )
    }
}

/**
 * Control panel with directional buttons only
 * Clean, centered layout
 */
@Composable
fun ControlPanel(viewModel: CarControlViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "🎮 Car Controls",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Hold buttons to move",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Movement Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Forward button (top)
                HoldControlButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "Forward",
                    onPress = { viewModel.moveForward() },
                    onRelease = { viewModel.stop() },
                    modifier = Modifier.size(80.dp),
                    containerColor = Color(0xFF2196F3) // Blue
                )

                Spacer(Modifier.height(16.dp))

                // Left, Stop, Right (middle row)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldControlButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Left",
                        onPress = { viewModel.turnLeft() },
                        onRelease = { viewModel.stop() },
                        modifier = Modifier.size(80.dp),
                        containerColor = Color(0xFF9C27B0) // Purple
                    )

                    ControlButton(
                        icon = Icons.Default.Close,
                        label = "Stop",
                        onClick = { viewModel.stop() },
                        modifier = Modifier.size(80.dp),
                        containerColor = Color(0xFFF44336) // Red
                    )

                    HoldControlButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = "Right",
                        onPress = { viewModel.turnRight() },
                        onRelease = { viewModel.stop() },
                        modifier = Modifier.size(80.dp),
                        containerColor = Color(0xFF9C27B0) // Purple
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Backward button (bottom)
                HoldControlButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    label = "Backward",
                    onPress = { viewModel.moveBackward() },
                    onRelease = { viewModel.stop() },
                    modifier = Modifier.size(80.dp),
                    containerColor = Color(0xFFFF9800) // Orange
                )
            }
        }
    }
}

/**
 * Hold-to-move control button component
 * Sends command when pressed and stop command when released
 */
@Composable
fun HoldControlButton(
    icon: ImageVector,
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF2196F3)
) {
    var isPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = modifier
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onPress()
                            tryAwaitRelease()
                            isPressed = false
                            onRelease()
                        }
                    )
                },
            color = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor,
            shadowElevation = if (isPressed) 2.dp else 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Reusable control button component (for Stop button - single click)
 */
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFF44336)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = modifier.clip(CircleShape),
            color = containerColor,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White
        )
    }
}

/**
 * Dialog to select Bluetooth device from paired list
 */
@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    selectedDevice: BluetoothDevice?,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select ESP32 Device",
                color = Color(0xFF00ACC1),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Text(
                        "No paired devices found. Please pair your ESP32 in Android Bluetooth settings first.",
                        color = Color(0xFFE53935)
                    )
                } else {
                    LazyColumn {
                        items(devices) { device ->
                            DeviceListItem(
                                device = device,
                                isSelected = device.address == selectedDevice?.address,
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) {
                Text("Refresh", color = Color(0xFF00ACC1))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF757575))
            }
        }
    )
}

/**
 * Single device item in the list
 */
@Composable
fun DeviceListItem(
    device: BluetoothDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF00ACC1).copy(alpha = 0.2f)
            else
                Color.Transparent
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00ACC1))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00ACC1) else Color(0xFF757575)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" },
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF00ACC1) else Color.Black
                )
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}