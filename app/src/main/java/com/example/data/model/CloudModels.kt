package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudMediaItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mediaType: String = "PHOTO", // PHOTO, VIDEO, NOTE, AUDIO
    val albumName: String = "Default",
    val originalFileName: String = "",
    val fileSizeBytes: Long = 0L,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val ivBase64: String = "",
    val encryptedDataPayload: String = "",
    val checksum: String = "",
    val isFavorite: Boolean = false,
    val isDecoy: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VaultSetting(
    val id: String = "default",
    val userId: String = "",
    val backupEnabled: Boolean = true,
    @ServerTimestamp val lastBackupTime: Timestamp? = null,
    val totalBackupSizeBytes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
