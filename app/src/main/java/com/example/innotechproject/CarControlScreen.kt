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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Main screen for cargi control
 * Shows device selection and control buttons
 */
@Composable
fun CarControlScreen(modifier: Modifier = Modifier, viewModel: CarControlViewModel = viewModel()) {


    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    val sliderPosition by viewModel.sliderPosition.collectAsState()



    var showDeviceDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.loadPairedDevices()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header
        Text(
            text = "Car Controller",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Connection Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    Color(0xFFF5F5F5)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
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
                            Color(0xFFF44336)
                        else
                            Color.Black
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
                    color = if (isConnected) Color(0xFF4CAF50) else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Control Panel (only enabled when connected)
        if (isConnected) {

            ControlPanel(viewModel, sliderPosition)

        } else {
            // Show placeholder when not connected
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
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
                            tint = Color.Black.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Connect to ESP32 to control the car",
                            color = Color.Black.copy(alpha = 0.7f)
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
 * Control panel with directional buttons
 * D-pad style layout: Forward, Back, Left, Right, Stop
 * Buttons use press/hold functionality - car moves while button is held
 */
@Composable
fun ControlPanel(viewModel: CarControlViewModel, sliderPosition: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Car Controls",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Hold buttons to move",
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Forward button (top)
            HoldControlButton(
                icon = Icons.Default.KeyboardArrowUp,
                label = "Forward",
                onPress = { viewModel.moveForward() },
                onRelease = { viewModel.stop() },
                modifier = Modifier.size(65.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Left, Stop, Right (middle row)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HoldControlButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Left",
                    onPress = { viewModel.turnLeft() },
                    onRelease = { viewModel.stop() },
                    modifier = Modifier.size(65.dp)
                )

                ControlButton(
                    icon = Icons.Default.Close,
                    label = "Stop",
                    onClick = { viewModel.stop() },
                    modifier = Modifier.size(65.dp),
                    containerColor = Color(0xFFF44336)
                )

                HoldControlButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = "Right",
                    onPress = { viewModel.turnRight() },
                    onRelease = { viewModel.stop() },
                    modifier = Modifier.size(65.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Backward button (bottom)
            HoldControlButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "Backward",
                onPress = { viewModel.moveBackward() },
                onRelease = { viewModel.stop() },
                modifier = Modifier.size(65.dp)
            )

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(color = Color.Black.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

            // Servo Control Section
            ServoControl(
                angle = sliderPosition,
                onAngleChange = { viewModel.setServoAngle(it) }
            )
        }
    }
}

/**
 * Servo motor control with slider (0-180 degrees)
 */
@Composable
fun ServoControl(
    angle: Float,
    onAngleChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Servo Motor Control",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Angle: ${angle.toInt()}°",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = angle,
            onValueChange = onAngleChange,
            valueRange = 0f..180f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.Black
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0°", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
            Text("90°", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
            Text("180°", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f))
        }

        Spacer(Modifier.height(12.dp))

        // Quick angle buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickAngleButton("0°", 0f, onAngleChange)
            QuickAngleButton("45°", 45f, onAngleChange)
            QuickAngleButton("90°", 90f, onAngleChange)
            QuickAngleButton("135°", 135f, onAngleChange)
            QuickAngleButton("180°", 180f, onAngleChange)
        }
    }
}

@Composable
fun QuickAngleButton(label: String, angle: Float, onClick: (Float) -> Unit) {
    Button(
        onClick = { onClick(angle) },
        modifier = Modifier.size(width = 60.dp, height = 40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Color.White)
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
    containerColor: Color = Color.Black
) {
    // Track if button is currently pressed
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
                            // Button pressed down
                            isPressed = true
                            onPress()
                            // Wait for button release
                            tryAwaitRelease()
                            // Button released
                            isPressed = false
                            onRelease()
                        }
                    )
                },
            color = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor,
            shadowElevation = if (isPressed) 2.dp else 4.dp
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
            fontSize = 12.sp,
            color = Color.Black,
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
    containerColor: Color = Color.Black
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = modifier.clip(CircleShape),
            color = containerColor,
            shadowElevation = 4.dp
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
            fontSize = 12.sp,
            color = Color.Black
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
        title = { Text("Select ESP32 Device") },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Text(
                        "No paired devices found. Please pair your ESP32 in Android Bluetooth settings first.",
                        color = MaterialTheme.colorScheme.error
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
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" },
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}