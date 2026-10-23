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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCrimson
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun LockScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val isConfigured = remember { viewModel.securityManager.isPasscodeConfigured() }
    var isSetupMode by remember { mutableStateOf(!isConfigured) }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter new PIN, 2: Confirm PIN
    var initialPin by remember { mutableStateOf("") }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCalculatorMode by remember { mutableStateOf(viewModel.securityManager.isCalculatorDisguiseEnabled()) }

    val failedAttempts by viewModel.securityManager.failedAttempts.collectAsState()
    val calcState by viewModel.calculatorState.collectAsState()

    // Auto-prompt biometrics if configured
    LaunchedEffect(Unit) {
        if (!isSetupMode && viewModel.securityManager.isBiometricsEnabled() && !isCalculatorMode && activity != null) {
            viewModel.unlockWithBiometrics(activity)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = VaultObsidian
    ) {
        if (isCalculatorMode) {
            CalculatorDisguiseView(
                calcState = calcState,
                onKey = { key ->
                    viewModel.onCalculatorKey(key)
                },
                onSwitchToPin = {
                    isCalculatorMode = false
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(VaultSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Status",
                            tint = VaultCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Calculator Disguise quick toggle
                    IconButton(
                        onClick = { isCalculatorMode = true },
                        modifier = Modifier.testTag("calculator_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Switch to Calculator Disguise",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Middle: Lock Header & PIN Indicators
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(VaultCyan.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                            .border(1.dp, VaultBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault Lock",
                            tint = VaultCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            isSetupMode && setupStep == 1 -> "Create Master Passcode"
                            isSetupMode && setupStep == 2 -> "Confirm Master Passcode"
                            else -> "Gallery Vault"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isSetupMode && setupStep == 1 -> "Enter 4 to 6 digits to secure your private vault"
                            isSetupMode && setupStep == 2 -> "Re-enter your passcode to confirm"
                            else -> "Protected by AES-256 Military Encryption"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Failed attempts intruder banner
                    if (failedAttempts > 0 && !isSetupMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VaultCrimson.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultCrimson.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Intruder Alert",
                                    tint = VaultCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "$failedAttempts failed unlock attempt${if (failedAttempts > 1) "s" else ""} logged",
                                    color = VaultCrimson,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = VaultCrimson,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayCount = maxOf(4, enteredPin.length)
                        for (i in 0 until displayCount) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) VaultCyan else Color.Transparent
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) VaultCyan else VaultBorder,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Keypad
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.6f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (key == "BIO" || key == "DEL") Color.Transparent else VaultSurfaceCard
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (key == "BIO" || key == "DEL") Color.Transparent else VaultBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            when (key) {
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                }
                                                "BIO" -> {
                                                    if (activity != null) {
                                                        viewModel.unlockWithBiometrics(activity)
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 8) {
                                                        val newPin = enteredPin + key
                                                        enteredPin = newPin
                                                        errorMessage = null

                                                        // Auto submit when reaching 4-6 digits in unlock mode
                                                        if (!isSetupMode && newPin.length >= 4) {
                                                            val success = viewModel.unlockWithPasscode(newPin)
                                                            if (!success && newPin.length >= 6) {
                                                                errorMessage = "Incorrect passcode"
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (key) {
                                        "DEL" -> Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        "BIO" -> if (!isSetupMode && viewModel.securityManager.canAuthenticateBiometrics()) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = VaultCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        else -> Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Setup button when in setup mode
                    if (isSetupMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (enteredPin.length < 4) {
                                    errorMessage = "PIN must be at least 4 digits"
                                    return@Button
                                }
                                if (setupStep == 1) {
                                    initialPin = enteredPin
                                    enteredPin = ""
                                    setupStep = 2
                                    errorMessage = null
                                } else {
                                    if (enteredPin == initialPin) {
                                        viewModel.securityManager.setupMasterPasscode(enteredPin)
                                        viewModel.unlockWithPasscode(enteredPin)
                                    } else {
                                        errorMessage = "Passcodes do not match. Try again."
                                        enteredPin = ""
                                        setupStep = 1
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("setup_continue_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (setupStep == 1) "Continue" else "Save & Open Vault",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorDisguiseView(
    calcState: com.example.ui.viewmodel.CalculatorState,
    onKey: (String) -> Unit,
    onSwitchToPin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultObsidian)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSwitchToPin) {
                    Text("Standard PIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Text(
                    text = "RAD",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (calcState.formulaText.isNotEmpty()) {
                Text(
                    text = calcState.formulaText,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = calcState.displayText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        // Calculator Buttons
        val rows = listOf(
            listOf("C", "( )", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("DEL", "0", ".", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = key in listOf("C", "( )", "%", "DEL")

                        val bgColor = when {
                            key == "=" -> VaultCyan
                            isOperator -> VaultPurple.copy(alpha = 0.3f)
                            isSpecial -> VaultSurfaceVariant
                            else -> VaultSurfaceCard
                        }

                        val textColor = when {
                            key == "=" -> Color.Black
                            isOperator -> VaultCyan
                            isSpecial -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .border(1.dp, VaultBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable { onKey(key) }
                                .testTag("calc_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = if (key == "=" || isOperator) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
