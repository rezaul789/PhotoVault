package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntruderAttempt(
    val timestamp: Long,
    val formattedTime: String,
    val attemptedCodeLength: Int
)

enum class UnlockResult {
    SUCCESS_MASTER,
    SUCCESS_DECOY,
    FAILED,
    LOCKED_OUT
}

class VaultSecurityManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    val biometricPromptManager = BiometricPromptManager(context)

    private val _failedAttempts = MutableStateFlow(prefs.getInt(KEY_FAILED_ATTEMPTS, 0))
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _intruderLogs = MutableStateFlow<List<IntruderAttempt>>(loadIntruderLogs())
    val intruderLogs: StateFlow<List<IntruderAttempt>> = _intruderLogs.asStateFlow()

    fun isPasscodeConfigured(): Boolean {
        return prefs.contains(KEY_MASTER_HASH)
    }

    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isCalculatorDisguiseEnabled(): Boolean {
        return prefs.getBoolean(KEY_CALCULATOR_DISGUISE, false)
    }

    fun setCalculatorDisguiseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALCULATOR_DISGUISE, enabled).apply()
    }

    fun isDecoyConfigured(): Boolean {
        return prefs.contains(KEY_DECOY_HASH)
    }

    fun setupMasterPasscode(passcode: String) {
        val salt = AesEncryptionManager.generateSalt()
        val hash = AesEncryptionManager.hashPasscode(passcode, salt)
        prefs.edit()
            .putString(KEY_MASTER_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_MASTER_HASH, hash)
            .apply()
    }

    fun setupDecoyPasscode(passcode: String) {
        val salt = AesEncryptionManager.generateSalt()
        val hash = AesEncryptionManager.hashPasscode(passcode, salt)
        prefs.edit()
            .putString(KEY_DECOY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_DECOY_HASH, hash)
            .apply()
    }

    fun clearDecoyPasscode() {
        prefs.edit()
            .remove(KEY_DECOY_SALT)
            .remove(KEY_DECOY_HASH)
            .apply()
    }

    fun verifyPasscode(input: String): UnlockResult {
        // Check Master PIN
        val masterSaltStr = prefs.getString(KEY_MASTER_SALT, null)
        val masterHash = prefs.getString(KEY_MASTER_HASH, null)

        if (masterSaltStr != null && masterHash != null) {
            val salt = Base64.decode(masterSaltStr, Base64.NO_WRAP)
            val testHash = AesEncryptionManager.hashPasscode(input, salt)
            if (testHash == masterHash) {
                // Success master
                resetFailedAttempts()
                return UnlockResult.SUCCESS_MASTER
            }
        }

        // Check Decoy PIN
        val decoySaltStr = prefs.getString(KEY_DECOY_SALT, null)
        val decoyHash = prefs.getString(KEY_DECOY_HASH, null)

        if (decoySaltStr != null && decoyHash != null) {
            val salt = Base64.decode(decoySaltStr, Base64.NO_WRAP)
            val testHash = AesEncryptionManager.hashPasscode(input, salt)
            if (testHash == decoyHash) {
                // Decoy vault accessed
                return UnlockResult.SUCCESS_DECOY
            }
        }

        // Record failure
        recordFailedAttempt(input.length)
        return UnlockResult.FAILED
    }

    private fun recordFailedAttempt(attemptedLength: Int) {
        val count = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, count).apply()
        _failedAttempts.value = count

        val timestamp = System.currentTimeMillis()
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        val newAttempt = IntruderAttempt(timestamp, formatter.format(Date(timestamp)), attemptedLength)

        val updated = listOf(newAttempt) + _intruderLogs.value.take(49)
        saveIntruderLogs(updated)
        _intruderLogs.value = updated
    }

    fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
        _failedAttempts.value = 0
    }

    fun clearIntruderLogs() {
        prefs.edit().remove(KEY_INTRUDER_LOGS).apply()
        _intruderLogs.value = emptyList()
    }

    private fun loadIntruderLogs(): List<IntruderAttempt> {
        val jsonStr = prefs.getString(KEY_INTRUDER_LOGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<IntruderAttempt>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    IntruderAttempt(
                        timestamp = obj.getLong("timestamp"),
                        formattedTime = obj.getString("formattedTime"),
                        attemptedCodeLength = obj.optInt("codeLength", 4)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveIntruderLogs(list: List<IntruderAttempt>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject().apply {
                put("timestamp", it.timestamp)
                put("formattedTime", it.formattedTime)
                put("codeLength", it.attemptedCodeLength)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_INTRUDER_LOGS, array.toString()).apply()
    }

    fun canAuthenticateBiometrics(): Boolean {
        return biometricPromptManager.canAuthenticate()
    }

    fun getBiometricStatus(): BiometricStatus {
        return biometricPromptManager.checkBiometricStatus()
    }

    fun launchBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Gallery Vault",
        subtitle: String = "Use fingerprint or face recognition to unlock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val config = BiometricPromptConfig(
            title = title,
            subtitle = subtitle,
            description = "Confirm your identity to unlock private vault",
            negativeButtonText = "Use PIN"
        )
        biometricPromptManager.showBiometricPrompt(
            activity = activity,
            config = config
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    resetFailedAttempts()
                    onSuccess()
                }
                is BiometricResult.Error -> {
                    onError(result.errorMessage)
                }
                is BiometricResult.Failed -> {
                    recordFailedAttempt(0)
                    onError("Biometric authentication failed")
                }
                is BiometricResult.Cancelled -> {
                    // User dismissed prompt or chose PIN option
                }
            }
        }
    }

    companion object {
        private const val KEY_MASTER_HASH = "master_hash"
        private const val KEY_MASTER_SALT = "master_salt"
        private const val KEY_DECOY_HASH = "decoy_hash"
        private const val KEY_DECOY_SALT = "decoy_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_CALCULATOR_DISGUISE = "calc_disguise"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_INTRUDER_LOGS = "intruder_logs"
    }
}
