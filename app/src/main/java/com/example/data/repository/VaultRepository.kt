package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.VaultDatabase
import com.example.data.local.VaultMediaDao
import com.example.data.local.VaultMediaEntity
import com.example.security.AesEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val dao: VaultMediaDao
) {
    private val vaultMediaDir = File(context.filesDir, "vault_media").apply {
        if (!exists()) mkdirs()
    }

    fun observeAll(isDecoy: Boolean): Flow<List<VaultMediaEntity>> = dao.observeAll(isDecoy)
    fun observeByType(mediaType: String, isDecoy: Boolean): Flow<List<VaultMediaEntity>> = dao.observeByType(mediaType, isDecoy)
    fun observeFavorites(isDecoy: Boolean): Flow<List<VaultMediaEntity>> = dao.observeFavorites(isDecoy)
    fun observeByAlbum(album: String, isDecoy: Boolean): Flow<List<VaultMediaEntity>> = dao.observeByAlbum(album, isDecoy)
    fun observeTrash(isDecoy: Boolean): Flow<List<VaultMediaEntity>> = dao.observeTrash(isDecoy)
    fun observeAlbums(isDecoy: Boolean): Flow<List<String>> = dao.observeAlbums(isDecoy)
    fun observeTotalCount(): Flow<Int> = dao.observeTotalItemsCount()
    fun observeSyncedCount(): Flow<Int> = dao.observeSyncedItemsCount()

    suspend fun getById(id: String): VaultMediaEntity? = dao.getById(id)

    suspend fun getAllForBackup(): List<VaultMediaEntity> = dao.getAllForBackup()

    suspend fun importMediaFromUri(
        uri: Uri,
        contentResolver: ContentResolver,
        mediaType: String = "PHOTO",
        albumName: String = "Default",
        isDecoy: Boolean = false
    ): Result<VaultMediaEntity> = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val id = UUID.randomUUID().toString()
            val encFileName = "$id.enc"
            val targetEncFile = File(vaultMediaDir, encFileName)

            val encryptResult = contentResolver.openInputStream(uri)?.use { inputStream ->
                AesEncryptionManager.encryptFile(inputStream, targetEncFile)
            } ?: return@withContext Result.failure(IllegalStateException("Could not read media from URI"))

            val entity = VaultMediaEntity(
                id = id,
                title = fileName.substringBeforeLast('.'),
                mediaType = mediaType,
                albumName = albumName,
                originalFileName = fileName,
                fileSizeBytes = if (fileSize > 0) fileSize else targetEncFile.length(),
                encryptedFileName = encFileName,
                ivBase64 = encryptResult.ivBase64,
                checksum = encryptResult.checksum,
                isFavorite = false,
                isDecoy = isDecoy,
                isTrash = false,
                cloudSyncStatus = "PENDING_BACKUP"
            )

            dao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSecretNote(
        title: String,
        content: String,
        albumName: String = "Notes",
        isDecoy: Boolean = false
    ): Result<VaultMediaEntity> = withContext(Dispatchers.IO) {
        try {
            val id = UUID.randomUUID().toString()
            val encFileName = "$id.enc"
            val targetEncFile = File(vaultMediaDir, encFileName)

            val contentBytes = content.toByteArray(Charsets.UTF_8)
            val encryptResult = ByteArrayInputStream(contentBytes).use { inputStream ->
                AesEncryptionManager.encryptFile(inputStream, targetEncFile)
            }

            val entity = VaultMediaEntity(
                id = id,
                title = title,
                mediaType = "NOTE",
                albumName = albumName,
                originalFileName = "$title.txt",
                fileSizeBytes = contentBytes.size.toLong(),
                encryptedFileName = encFileName,
                ivBase64 = encryptResult.ivBase64,
                checksum = encryptResult.checksum,
                noteContent = null, // Stored encrypted on disk
                isFavorite = false,
                isDecoy = isDecoy,
                isTrash = false,
                cloudSyncStatus = "PENDING_BACKUP"
            )

            dao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptNote(entity: VaultMediaEntity): String = withContext(Dispatchers.IO) {
        val encFile = File(vaultMediaDir, entity.encryptedFileName)
        if (!encFile.exists()) return@withContext ""
        val bytes = AesEncryptionManager.decryptFile(encFile)
        String(bytes, Charsets.UTF_8)
    }

    suspend fun decryptToBitmap(entity: VaultMediaEntity): Bitmap? = withContext(Dispatchers.IO) {
        val encFile = File(vaultMediaDir, entity.encryptedFileName)
        if (!encFile.exists()) return@withContext null
        AesEncryptionManager.decryptToBitmap(encFile)
    }

    suspend fun getEncryptedBytes(entity: VaultMediaEntity): ByteArray = withContext(Dispatchers.IO) {
        val encFile = File(vaultMediaDir, entity.encryptedFileName)
        if (!encFile.exists()) ByteArray(0) else encFile.readBytes()
    }

    suspend fun moveToTrash(id: String) = dao.setTrash(id, true)
    suspend fun restoreFromTrash(id: String) = dao.setTrash(id, false)
    suspend fun toggleFavorite(id: String, current: Boolean) = dao.setFavorite(id, !current)
    suspend fun moveAlbum(id: String, newAlbum: String) = dao.moveAlbum(id, newAlbum)

    suspend fun deletePermanently(id: String) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id)
        if (entity != null) {
            val encFile = File(vaultMediaDir, entity.encryptedFileName)
            if (encFile.exists()) encFile.delete()
            dao.deleteById(id)
        }
    }

    suspend fun emptyTrash(isDecoy: Boolean) = withContext(Dispatchers.IO) {
        val trashed = dao.observeTrash(isDecoy)
        // Trash items file deletion
        dao.emptyTrash(isDecoy)
    }

    suspend fun markSynced(id: String) {
        dao.updateSyncStatus(id, "SYNCED")
    }

    suspend fun restoreFromCloudItem(
        cloudItem: com.example.data.model.CloudMediaItem,
        encryptedPayload: ByteArray
    ) = withContext(Dispatchers.IO) {
        val encFileName = "${cloudItem.id}.enc"
        val targetEncFile = File(vaultMediaDir, encFileName)
        targetEncFile.writeBytes(encryptedPayload)

        val entity = VaultMediaEntity(
            id = cloudItem.id,
            title = cloudItem.title,
            mediaType = cloudItem.mediaType,
            albumName = cloudItem.albumName,
            originalFileName = cloudItem.originalFileName,
            fileSizeBytes = cloudItem.fileSizeBytes,
            encryptedFileName = encFileName,
            ivBase64 = cloudItem.ivBase64,
            checksum = cloudItem.checksum,
            isFavorite = cloudItem.isFavorite,
            isDecoy = cloudItem.isDecoy,
            isTrash = false,
            cloudSyncStatus = "SYNCED",
            createdAt = cloudItem.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = cloudItem.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
        )
        dao.insert(entity)
    }
}
