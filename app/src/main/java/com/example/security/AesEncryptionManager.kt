package com.example.security

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedResult(
    val encryptedBytes: ByteArray,
    val iv: ByteArray,
    val checksum: String
) {
    val ivBase64: String get() = Base64.encodeToString(iv, Base64.NO_WRAP)
    val encryptedPayloadBase64: String get() = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedResult
        return encryptedBytes.contentEquals(other.encryptedBytes) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

object AesEncryptionManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "VaultMasterKey_v1"
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_LENGTH_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Derives a 256-bit AES key from user passcode and salt using PBKDF2.
     */
    fun deriveKeyFromPasscode(passcode: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts raw byte array using AES-256-GCM.
     */
    fun encrypt(data: ByteArray, customKey: SecretKey? = null): EncryptedResult {
        val key = customKey ?: ensureMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val iv = ByteArray(GCM_IV_LENGTH).apply { secureRandom.nextBytes(this) }
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val encryptedBytes = cipher.doFinal(data)
        val checksum = sha256Checksum(data)
        return EncryptedResult(encryptedBytes, iv, checksum)
    }

    /**
     * Decrypts AES-256-GCM encrypted byte array.
     */
    fun decrypt(encryptedBytes: ByteArray, iv: ByteArray, customKey: SecretKey? = null): ByteArray {
        val key = customKey ?: ensureMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(encryptedBytes)
    }

    /**
     * Encrypts a source file and writes the encrypted payload to destination file.
     * File header: 12 bytes IV followed by encrypted bytes.
     */
    fun encryptFile(source: InputStream, destinationFile: File, customKey: SecretKey? = null): EncryptedResult {
        val data = source.readBytes()
        val result = encrypt(data, customKey)
        FileOutputStream(destinationFile).use { fos ->
            fos.write(result.iv)
            fos.write(result.encryptedBytes)
        }
        return result
    }

    /**
     * Decrypts a file (reading IV from the first 12 bytes) directly to memory without saving plain file on disk.
     */
    fun decryptFile(encryptedFile: File, customKey: SecretKey? = null): ByteArray {
        FileInputStream(encryptedFile).use { fis ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val bytesRead = fis.read(iv)
            if (bytesRead < GCM_IV_LENGTH) throw IllegalArgumentException("Corrupt encrypted file header")
            val encryptedBytes = fis.readBytes()
            return decrypt(encryptedBytes, iv, customKey)
        }
    }

    /**
     * Decrypts image directly to in-memory Bitmap.
     */
    fun decryptToBitmap(encryptedFile: File, customKey: SecretKey? = null): Bitmap? {
        val decryptedBytes = decryptFile(encryptedFile, customKey)
        return BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
    }

    fun generateSalt(): ByteArray {
        return ByteArray(16).apply { secureRandom.nextBytes(this) }
    }

    fun sha256Checksum(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hashPasscode(passcode: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(passcode.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
