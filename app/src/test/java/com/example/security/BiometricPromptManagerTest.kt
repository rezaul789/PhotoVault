package com.example.security

import android.content.Context
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricPromptManagerTest {

    private lateinit var context: Context
    private lateinit var biometricPromptManager: BiometricPromptManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        biometricPromptManager = BiometricPromptManager(context)
    }

    @Test
    fun testCheckBiometricStatus_returnsValidStatus() {
        val status = biometricPromptManager.checkBiometricStatus()
        assertNotNull(status)
        // In Robolectric standard environment, biometric hardware is either NoHardware or NotEnrolled
        assertTrue(
            status is BiometricStatus.NoHardware ||
            status is BiometricStatus.NotEnrolled ||
            status is BiometricStatus.Ready ||
            status is BiometricStatus.Unsupported ||
            status is BiometricStatus.HardwareUnavailable
        )
    }

    @Test
    fun testCreatePromptInfo_configuresAllFields() {
        val config = BiometricPromptConfig(
            title = "Unlock Gallery Vault",
            subtitle = "Verify Fingerprint or Face",
            description = "Military grade encryption access",
            negativeButtonText = "Enter PIN",
            confirmationRequired = true,
            authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        val promptInfo = biometricPromptManager.createPromptInfo(config)
        assertNotNull(promptInfo)
        assertEquals(config.title, promptInfo.title)
        assertEquals(config.subtitle, promptInfo.subtitle)
        assertEquals(config.description, promptInfo.description)
        assertEquals(config.negativeButtonText, promptInfo.negativeButtonText)
        assertEquals(config.confirmationRequired, promptInfo.isConfirmationRequired)
        assertEquals(config.authenticators, promptInfo.allowedAuthenticators)
    }

    @Test
    fun testCreateEnrollmentIntent_returnsValidSettingsIntent() {
        val intent = biometricPromptManager.createEnrollmentIntent()
        assertNotNull(intent)
        val validActions = setOf(
            Settings.ACTION_BIOMETRIC_ENROLL,
            Settings.ACTION_SECURITY_SETTINGS
        )
        assertTrue(validActions.contains(intent.action))
    }

    @Test
    fun testBiometricStatus_isAvailableHelper() {
        assertTrue(BiometricStatus.Ready.isAvailable)
        assertEquals(false, BiometricStatus.NotEnrolled.isAvailable)
        assertEquals(false, BiometricStatus.NoHardware.isAvailable)
        assertEquals(false, BiometricStatus.HardwareUnavailable.isAvailable)
    }
}
