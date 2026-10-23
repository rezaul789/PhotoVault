package com.example.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Status of biometric authentication availability on the current device.
 */
sealed class BiometricStatus {
    object Ready : BiometricStatus()
    object NotEnrolled : BiometricStatus()
    object NoHardware : BiometricStatus()
    object HardwareUnavailable : BiometricStatus()
    object SecurityUpdateRequired : BiometricStatus()
    data class Unsupported(val rawCode: Int) : BiometricStatus()

    val isAvailable: Boolean get() = this is Ready
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    data class Success(val result: BiometricPrompt.AuthenticationResult) : BiometricResult()
    data class Error(val errorCode: Int, val errorMessage: String) : BiometricResult()
    object Failed : BiometricResult()
    data class Cancelled(val byUser: Boolean) : BiometricResult()
}

/**
 * Configuration options for the BiometricPrompt modal.
 */
data class BiometricPromptConfig(
    val title: String = "Unlock Gallery Vault",
    val subtitle: String? = "Verify your fingerprint or face recognition",
    val description: String? = "Confirm your identity to access secured content",
    val negativeButtonText: String = "Use PIN",
    val confirmationRequired: Boolean = false,
    val authenticators: Int = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK,
    val cryptoObject: BiometricPrompt.CryptoObject? = null
)

/**
 * Production-ready utility class wrapping AndroidX Biometric library APIs
 * (`androidx.biometric.BiometricPrompt` & `androidx.biometric.BiometricManager`)
 * to handle fingerprint and facial authentication for application access.
 */
class BiometricPromptManager(private val context: Context) {

    private val biometricManager: BiometricManager = BiometricManager.from(context)

    /**
     * Checks if biometric authentication (fingerprint or face) is supported and ready to authenticate.
     */
    fun canAuthenticate(
        authenticators: Int = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
    ): Boolean {
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Inspects the detailed biometric status of the device (hardware existence, enrollment, availability).
     */
    fun checkBiometricStatus(
        authenticators: Int = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
    ): BiometricStatus {
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            else -> BiometricStatus.Unsupported(biometricManager.canAuthenticate(authenticators))
        }
    }

    /**
     * Shows a BiometricPrompt modal using [FragmentActivity].
     *
     * @param activity Hosting FragmentActivity
     * @param config Configuration options including prompt title, subtitle, and negative button
     * @param onResult Callback receiving [BiometricResult] (Success, Error, Failed, or Cancelled)
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        config: BiometricPromptConfig = BiometricPromptConfig(),
        onResult: (BiometricResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success(result))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(BiometricResult.Cancelled(byUser = true))
                    }
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricResult.Cancelled(byUser = false))
                    }
                    else -> {
                        onResult(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricResult.Failed)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = createPromptInfo(config)

        if (config.cryptoObject != null) {
            prompt.authenticate(promptInfo, config.cryptoObject)
        } else {
            prompt.authenticate(promptInfo)
        }

        return prompt
    }

    /**
     * Simplified convenience helper for direct lambda callbacks.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Gallery Vault",
        subtitle: String? = "Verify your fingerprint or face recognition",
        negativeButtonText: String = "Use PIN",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit,
        onFailed: () -> Unit,
        onCancelled: () -> Unit = {}
    ): BiometricPrompt {
        val config = BiometricPromptConfig(
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        )
        return showBiometricPrompt(activity, config) { result ->
            when (result) {
                is BiometricResult.Success -> onSuccess(result.result)
                is BiometricResult.Error -> onError(result.errorCode, result.errorMessage)
                is BiometricResult.Failed -> onFailed()
                is BiometricResult.Cancelled -> onCancelled()
            }
        }
    }

    /**
     * Coroutine-based authentication flow suspending until the user authenticates, cancels, or encounters an error.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        config: BiometricPromptConfig = BiometricPromptConfig()
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        var promptRef: BiometricPrompt? = null

        continuation.invokeOnCancellation {
            promptRef?.cancelAuthentication()
        }

        promptRef = showBiometricPrompt(activity, config) { result ->
            if (continuation.isActive) {
                if (result !is BiometricResult.Failed) {
                    continuation.resume(result)
                }
            }
        }
    }

    /**
     * Builds a [BiometricPrompt.PromptInfo] instance from the given [BiometricPromptConfig].
     */
    fun createPromptInfo(config: BiometricPromptConfig): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)
            .setConfirmationRequired(config.confirmationRequired)
            .setAllowedAuthenticators(config.authenticators)

        config.subtitle?.let { builder.setSubtitle(it) }
        config.description?.let { builder.setDescription(it) }

        val includesDeviceCredential = (config.authenticators and Authenticators.DEVICE_CREDENTIAL) != 0
        if (!includesDeviceCredential) {
            builder.setNegativeButtonText(config.negativeButtonText)
        }

        return builder.build()
    }

    /**
     * Returns an [Intent] to launch the system's biometric / security enrollment screen
     * if the user has no fingerprints or facial data enrolled.
     */
    fun createEnrollmentIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}
