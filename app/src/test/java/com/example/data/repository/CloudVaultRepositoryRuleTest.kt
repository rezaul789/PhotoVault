package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CloudMediaItem
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudVaultRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun backupMediaItem_authenticatedUser_succeeds() {
        runBlocking {
            val uid = signInTestUser("vaultuser@test.com")
            val repository = CloudVaultRepository(firestore, auth)
            val mediaId = "media_" + java.util.UUID.randomUUID().toString().take(8)

            val item = CloudMediaItem(
                id = mediaId,
                userId = uid,
                title = "Family Photo",
                mediaType = "PHOTO",
                albumName = "Family",
                originalFileName = "family.jpg",
                fileSizeBytes = 4096L,
                encryptionAlgorithm = "AES-256-GCM",
                ivBase64 = "dGVzdEl2MTIzNDU2",
                encryptedDataPayload = "ZW5jcnlwdGVkRGF0YQ==",
                checksum = "sha256hash123",
                isFavorite = true,
                isDecoy = false
            )

            withTimeout(5000L) {
                repository.backupMediaItem(item)
            }

            val items = withTimeout(5000L) {
                repository.fetchCloudItemsOnce()
            }
            assertTrue(items.any { it.id == mediaId && it.title == "Family Photo" })
        }
    }

    @Test
    fun backupMediaItem_unauthenticated_fails() {
        runBlocking {
            auth.signOut()
            val repository = CloudVaultRepository(firestore, auth)

            val item = CloudMediaItem(
                id = "test_media_unauthed",
                userId = "someone_else",
                title = "Unauth Photo"
            )

            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.backupMediaItem(item)
                }
            }
        }
    }
}
