package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaType: String, // PHOTO, VIDEO, NOTE, AUDIO
    val albumName: String,
    val originalFileName: String,
    val fileSizeBytes: Long,
    val encryptedFileName: String,
    val ivBase64: String,
    val checksum: String,
    val noteContent: String? = null,
    val isFavorite: Boolean = false,
    val isDecoy: Boolean = false,
    val isTrash: Boolean = false,
    val cloudSyncStatus: String = "LOCAL_ONLY", // LOCAL_ONLY, SYNCED, PENDING_BACKUP
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface VaultMediaDao {
    @Query("SELECT * FROM vault_media WHERE isTrash = 0 AND isDecoy = :isDecoy ORDER BY createdAt DESC")
    fun observeAll(isDecoy: Boolean): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isTrash = 0 AND isDecoy = :isDecoy AND mediaType = :mediaType ORDER BY createdAt DESC")
    fun observeByType(mediaType: String, isDecoy: Boolean): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isTrash = 0 AND isDecoy = :isDecoy AND isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(isDecoy: Boolean): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isTrash = 0 AND isDecoy = :isDecoy AND albumName = :albumName ORDER BY createdAt DESC")
    fun observeByAlbum(albumName: String, isDecoy: Boolean): Flow<List<VaultMediaEntity>>

    @Query("SELECT * FROM vault_media WHERE isTrash = 1 AND isDecoy = :isDecoy ORDER BY updatedAt DESC")
    fun observeTrash(isDecoy: Boolean): Flow<List<VaultMediaEntity>>

    @Query("SELECT DISTINCT albumName FROM vault_media WHERE isTrash = 0 AND isDecoy = :isDecoy")
    fun observeAlbums(isDecoy: Boolean): Flow<List<String>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultMediaEntity?

    @Query("SELECT * FROM vault_media WHERE isTrash = 0 AND isDecoy = 0")
    suspend fun getAllForBackup(): List<VaultMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VaultMediaEntity>)

    @Update
    suspend fun update(entity: VaultMediaEntity)

    @Query("UPDATE vault_media SET isTrash = :isTrash, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTrash(id: String, isTrash: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE vault_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE vault_media SET albumName = :newAlbum WHERE id = :id")
    suspend fun moveAlbum(id: String, newAlbum: String)

    @Query("UPDATE vault_media SET cloudSyncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM vault_media WHERE isTrash = 1 AND isDecoy = :isDecoy")
    suspend fun emptyTrash(isDecoy: Boolean)

    @Query("SELECT COUNT(*) FROM vault_media WHERE isTrash = 0 AND isDecoy = 0")
    fun observeTotalItemsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_media WHERE isTrash = 0 AND isDecoy = 0 AND cloudSyncStatus = 'SYNCED'")
    fun observeSyncedItemsCount(): Flow<Int>
}

@Database(entities = [VaultMediaEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultMediaDao(): VaultMediaDao
}
