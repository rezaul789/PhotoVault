package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudMediaItem
import com.example.data.model.VaultSetting
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudVaultRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Secondary constructor resolving named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing cloud backup.")
    }

    fun observeCloudItems(): Flow<List<CloudMediaItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/cloud_vault"
        emitAll(
            db.collection("users").document(uid).collection("cloud_vault")
                .snapshots()
                .map { snapshot -> snapshot.toObjects<CloudMediaItem>() }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun fetchCloudItemsOnce(): List<CloudMediaItem> {
        val uid = requireUserId()
        val path = "users/$uid/cloud_vault"
        return try {
            val snapshot = db.collection("users").document(uid).collection("cloud_vault").get().await()
            snapshot.toObjects<CloudMediaItem>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun backupMediaItem(item: CloudMediaItem) {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "Item ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("cloud_vault").document(item.id)

        try {
            val payload = mapOf(
                "id" to item.id,
                "userId" to uid,
                "title" to item.title,
                "mediaType" to item.mediaType,
                "albumName" to item.albumName,
                "originalFileName" to item.originalFileName,
                "fileSizeBytes" to item.fileSizeBytes,
                "encryptionAlgorithm" to item.encryptionAlgorithm,
                "ivBase64" to item.ivBase64,
                "encryptedDataPayload" to item.encryptedDataPayload,
                "checksum" to item.checksum,
                "isFavorite" to item.isFavorite,
                "isDecoy" to item.isDecoy,
                "createdAt" to (item.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCloudMediaItem(mediaId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cloud_vault").document(mediaId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeVaultSettings(): Flow<VaultSetting?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/vault_settings/default"
        emitAll(
            db.collection("users").document(uid).collection("vault_settings").document("default")
                .snapshots()
                .map { snapshot -> snapshot.toObject<VaultSetting>() }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveVaultSettings(backupEnabled: Boolean, totalSizeBytes: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("vault_settings").document("default")
        try {
            val payload = mapOf(
                "id" to "default",
                "userId" to uid,
                "backupEnabled" to backupEnabled,
                "totalBackupSizeBytes" to totalSizeBytes,
                "lastBackupTime" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
