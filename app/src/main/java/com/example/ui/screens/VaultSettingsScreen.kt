package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCrimson
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun VaultSettingsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val securityManager = viewModel.securityManager
    var biometricsEnabled by remember { mutableStateOf(securityManager.isBiometricsEnabled()) }
    var calculatorDisguiseEnabled by remember { mutableStateOf(securityManager.isCalculatorDisguiseEnabled()) }
    var isDecoySet by remember { mutableStateOf(securityManager.isDecoyConfigured()) }

    val intruderLogs by securityManager.intruderLogs.collectAsState()
    val trashedItems by viewModel.trashedItems.collectAsState()

    var showChangeMasterPinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showIntruderLogsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VaultObsidian)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Security Overview Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(VaultCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = VaultCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Security & Stealth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Biometrics, Decoy Vault & Intruder Protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section: Authentication & Access
        Text(
            text = "ACCESS & LOCK",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = VaultCyan,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
        ) {
            Column {
                // Change Master Passcode
                SettingRow(
                    icon = Icons.Default.Key,
                    title = "Change Master Passcode",
                    subtitle = "Update your primary vault PIN",
                    onClick = { showChangeMasterPinDialog = true },
                    testTag = "change_pin_setting"
                )

                DividerLine()

                // Biometric Unlock Switch
                val context = androidx.compose.ui.platform.LocalContext.current
                val bioStatus = remember { securityManager.getBiometricStatus() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometrics",
                            tint = when (bioStatus) {
                                is com.example.security.BiometricStatus.Ready -> VaultCyan
                                is com.example.security.BiometricStatus.NotEnrolled -> VaultAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Biometric Authentication",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when (bioStatus) {
                                    is com.example.security.BiometricStatus.Ready -> "Ready: Fingerprint or face recognition"
                                    is com.example.security.BiometricStatus.NotEnrolled -> "Sensor available (Tap to enroll in Settings)"
                                    is com.example.security.BiometricStatus.NoHardware -> "No biometric sensor detected"
                                    is com.example.security.BiometricStatus.HardwareUnavailable -> "Sensor temporarily unavailable"
                                    else -> "Unlock with fingerprint or face recognition"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (bioStatus) {
                                    is com.example.security.BiometricStatus.Ready -> VaultEmerald
                                    is com.example.security.BiometricStatus.NotEnrolled -> VaultAmber
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = if (bioStatus is com.example.security.BiometricStatus.NotEnrolled) {
                                    Modifier.clickable {
                                        try {
                                            context.startActivity(securityManager.biometricPromptManager.createEnrollmentIntent())
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    }
                                } else Modifier
                            )
                        }
                    }
                    Switch(
                        checked = biometricsEnabled && bioStatus !is com.example.security.BiometricStatus.NoHardware,
                        enabled = bioStatus !is com.example.security.BiometricStatus.NoHardware,
                        onCheckedChange = {
                            biometricsEnabled = it
                            securityManager.setBiometricsEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = VaultCyan
                        ),
                        modifier = Modifier.testTag("biometric_switch")
                    )
                }

                DividerLine()

                // Stealth Calculator Disguise
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator Disguise",
                            tint = VaultPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Calculator Disguise Mode",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "App opens as a real calculator. Type PIN + '=' to unlock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = calculatorDisguiseEnabled,
                        onCheckedChange = {
                            calculatorDisguiseEnabled = it
                            securityManager.setCalculatorDisguiseEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = VaultPurple
                        ),
                        modifier = Modifier.testTag("calculator_disguise_switch")
                    )
                }
            }
        }

        // Section: Stealth & Duress (Decoy Vault)
        Text(
            text = "DURESS & DECOY PROTECTION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = VaultAmber,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
        ) {
            Column {
                SettingRow(
                    icon = Icons.Default.LockOpen,
                    title = if (isDecoySet) "Decoy (Fake) PIN Configured" else "Set Up Decoy (Fake) PIN",
                    subtitle = "Entering this PIN opens a fake, decoy vault with dummy photos when forced to unlock under duress",
                    trailingText = if (isDecoySet) "Active" else "Disabled",
                    trailingColor = if (isDecoySet) VaultEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { showDecoyPinDialog = true },
                    testTag = "decoy_pin_setting"
                )

                DividerLine()

                // Intruder Alert Logs
                SettingRow(
                    icon = Icons.Default.Warning,
                    title = "Break-in Attempts & Logs",
                    subtitle = "${intruderLogs.size} failed unlock events recorded",
                    trailingText = if (intruderLogs.isNotEmpty()) "${intruderLogs.size} Alerts" else "Clean",
                    trailingColor = if (intruderLogs.isNotEmpty()) VaultCrimson else VaultEmerald,
                    onClick = { showIntruderLogsDialog = true },
                    testTag = "intruder_logs_setting"
                )
            }
        }

        // Section: Storage & Maintenance
        Text(
            text = "STORAGE & TRASH",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = VaultCyan,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
        ) {
            Column {
                SettingRow(
                    icon = Icons.Default.Delete,
                    title = "Recycle Bin",
                    subtitle = "${trashedItems.size} items in trash",
                    onClick = { showTrashDialog = true },
                    testTag = "trash_setting"
                )
            }
        }

        // Section: Cryptography Specs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Specs",
                        tint = VaultCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cryptographic Architecture",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• AES-256-GCM authenticated encryption\n• Hardware AndroidKeyStore key protection\n• PBKDF2 with HMAC-SHA256 passcode stretching\n• On-demand in-memory decryption (never decrypted to disk)\n• End-to-end encrypted Google Cloud backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }

    // Change Master PIN Dialog
    if (showChangeMasterPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangeMasterPinDialog = false },
            title = { Text("Change Master Passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN (4-6 digits)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm New PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) {
                        Text(err ?: "", color = VaultCrimson, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            err = "PIN must be at least 4 digits"
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            err = "PINs do not match"
                            return@Button
                        }
                        securityManager.setupMasterPasscode(newPin)
                        showChangeMasterPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeMasterPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Decoy PIN Dialog
    if (showDecoyPinDialog) {
        var decoyPinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDecoyPinDialog = false },
            title = { Text("Decoy (Fake) PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "When unlocked with this PIN, Gallery Vault will show a completely separate, decoy vault without your private photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = decoyPinInput,
                        onValueChange = { decoyPinInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Decoy PIN (e.g. 0000)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDecoySet) {
                        TextButton(
                            onClick = {
                                securityManager.clearDecoyPasscode()
                                isDecoySet = false
                                showDecoyPinDialog = false
                            }
                        ) {
                            Text("Clear Decoy", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            if (decoyPinInput.length >= 4) {
                                securityManager.setupDecoyPasscode(decoyPinInput)
                                isDecoySet = true
                                showDecoyPinDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAmber)
                    ) {
                        Text("Save Decoy", color = Color.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Intruder Logs Dialog
    if (showIntruderLogsDialog) {
        AlertDialog(
            onDismissRequest = { showIntruderLogsDialog = false },
            title = { Text("Break-in Intruder Logs") },
            text = {
                if (intruderLogs.isEmpty()) {
                    Text("No break-in attempts detected. Your vault has not been tampered with.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(intruderLogs) { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VaultSurfaceVariant)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Failed Attempt: ${log.formattedTime}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = VaultCrimson,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Code length entered: ${log.attemptedCodeLength} digits",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (intruderLogs.isNotEmpty()) {
                    Button(
                        onClick = {
                            securityManager.clearIntruderLogs()
                            showIntruderLogsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultCrimson)
                    ) {
                        Text("Clear Logs")
                    }
                } else {
                    TextButton(onClick = { showIntruderLogsDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (intruderLogs.isNotEmpty()) {
                    TextButton(onClick = { showIntruderLogsDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Trash Dialog
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Recycle Bin (${trashedItems.size})") },
            text = {
                if (trashedItems.isEmpty()) {
                    Text("Recycle bin is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trashedItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VaultSurfaceVariant)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Medium, maxLines = 1, color = MaterialTheme.colorScheme.onBackground)
                                    Text(item.albumName, fontSize = 11.sp, color = VaultCyan)
                                }

                                Row {
                                    IconButton(onClick = { viewModel.restoreFromTrash(item.id) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = VaultEmerald)
                                    }
                                    IconButton(onClick = { viewModel.deletePermanently(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (trashedItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.emptyTrash()
                            showTrashDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultCrimson)
                    ) {
                        Text("Empty Trash")
                    }
                } else {
                    TextButton(onClick = { showTrashDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (trashedItems.isNotEmpty()) {
                    TextButton(onClick = { showTrashDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    trailingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = VaultCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                color = trailingColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VaultBorder.copy(alpha = 0.5f))
    )
}
