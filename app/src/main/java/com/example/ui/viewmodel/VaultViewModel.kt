package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.R
import com.example.data.local.VaultDatabase
import com.example.data.local.VaultMediaEntity
import com.example.data.model.CloudMediaItem
import com.example.data.repository.CloudVaultRepository
import com.example.data.repository.VaultRepository
import com.example.security.UnlockResult
import com.example.security.VaultSecurityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VaultTab {
    PHOTOS,
    VIDEOS,
    NOTES,
    CLOUD,
    SETTINGS
}

data class CalculatorState(
    val displayText: String = "0",
    val formulaText: String = "",
    val lastResult: Double = 0.0
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        VaultDatabase::class.java,
        "vault_local.db"
    ).build()

    val localRepository = VaultRepository(application, db.vaultMediaDao())
    val securityManager = VaultSecurityManager(application)

    private val firestoreInstance = FirebaseFirestore.getInstance(
        application.getString(R.string.firestore_database_id)
    )
    val cloudRepository = CloudVaultRepository(firestoreInstance)

    // Auth & Lock State
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isDecoyVault = MutableStateFlow(false)
    val isDecoyVault: StateFlow<Boolean> = _isDecoyVault.asStateFlow()

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // Navigation & Filters
    private val _selectedTab = MutableStateFlow(VaultTab.PHOTOS)
    val selectedTab: StateFlow<VaultTab> = _selectedTab.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Status & Feedback
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    // Calculator Stealth Disguise State
    private val _calculatorState = MutableStateFlow(CalculatorState())
    val calculatorState: StateFlow<CalculatorState> = _calculatorState.asStateFlow()
    private var calcAccumulator = ""

    // Active media detail
    private val _selectedMedia = MutableStateFlow<VaultMediaEntity?>(null)
    val selectedMedia: StateFlow<VaultMediaEntity?> = _selectedMedia.asStateFlow()

    private val _decryptedBitmap = MutableStateFlow<Bitmap?>(null)
    val decryptedBitmap: StateFlow<Bitmap?> = _decryptedBitmap.asStateFlow()

    private val _decryptedNoteContent = MutableStateFlow<String?>(null)
    val decryptedNoteContent: StateFlow<String?> = _decryptedNoteContent.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
        }
    }

    // Media list Flow based on Decoy status and selected tab
    val mediaItems: StateFlow<List<VaultMediaEntity>> = combine(
        _isDecoyVault,
        _selectedTab,
        _selectedAlbum,
        _searchQuery
    ) { isDecoy, tab, album, query ->
        CombinedFilter(isDecoy, tab, album, query)
    }.flatMapLatest { filter ->
        when (filter.tab) {
            VaultTab.PHOTOS -> if (filter.album != null) localRepository.observeByAlbum(filter.album, filter.isDecoy) else localRepository.observeByType("PHOTO", filter.isDecoy)
            VaultTab.VIDEOS -> localRepository.observeByType("VIDEO", filter.isDecoy)
            VaultTab.NOTES -> localRepository.observeByType("NOTE", filter.isDecoy)
            else -> localRepository.observeAll(filter.isDecoy)
        }
    }.combine(_searchQuery) { list, q ->
        if (q.isBlank()) list else list.filter { it.title.contains(q, ignoreCase = true) || it.albumName.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<String>> = _isDecoyVault.flatMapLatest { isDecoy ->
        localRepository.observeAlbums(isDecoy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedItems: StateFlow<List<VaultMediaEntity>> = _isDecoyVault.flatMapLatest { isDecoy ->
        localRepository.observeTrash(isDecoy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalItemsCount = localRepository.observeTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedItemsCount = localRepository.observeSyncedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSelectedTab(tab: VaultTab) {
        _selectedTab.value = tab
    }

    fun setSelectedAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- Unlock & Security ---

    fun unlockWithPasscode(passcode: String): Boolean {
        return when (securityManager.verifyPasscode(passcode)) {
            UnlockResult.SUCCESS_MASTER -> {
                _isDecoyVault.value = false
                _isLocked.value = false
                true
            }
            UnlockResult.SUCCESS_DECOY -> {
                _isDecoyVault.value = true
                _isLocked.value = false
                true
            }
            UnlockResult.FAILED, UnlockResult.LOCKED_OUT -> {
                _statusMessage.value = "Incorrect passcode. Attempt logged."
                false
            }
        }
    }

    fun unlockWithBiometrics(activity: FragmentActivity) {
        if (!securityManager.isBiometricsEnabled() || !securityManager.canAuthenticateBiometrics()) {
            _statusMessage.value = "Biometric authentication not configured"
            return
        }
        securityManager.launchBiometricPrompt(
            activity = activity,
            onSuccess = {
                _isDecoyVault.value = false
                _isLocked.value = false
            },
            onError = { error ->
                _statusMessage.value = error
            }
        )
    }

    fun lockVault() {
        _isLocked.value = true
        _selectedMedia.value = null
        _decryptedBitmap.value = null
        _decryptedNoteContent.value = null
        calcAccumulator = ""
        _calculatorState.value = CalculatorState()
    }

    // --- Calculator Disguise Key Handling ---

    fun onCalculatorKey(key: String) {
        when (key) {
            "C" -> {
                calcAccumulator = ""
                _calculatorState.value = CalculatorState("0", "", 0.0)
            }
            "DEL" -> {
                if (calcAccumulator.isNotEmpty()) {
                    calcAccumulator = calcAccumulator.dropLast(1)
                    _calculatorState.value = _calculatorState.value.copy(
                        displayText = if (calcAccumulator.isEmpty()) "0" else calcAccumulator
                    )
                }
            }
            "=" -> {
                // Check if calcAccumulator matches passcode!
                if (securityManager.verifyPasscode(calcAccumulator) in listOf(UnlockResult.SUCCESS_MASTER, UnlockResult.SUCCESS_DECOY)) {
                    unlockWithPasscode(calcAccumulator)
                    calcAccumulator = ""
                    return
                }
                // Otherwise evaluate standard math expression
                try {
                    val evaluated = evaluateSimpleMath(calcAccumulator)
                    _calculatorState.value = CalculatorState(
                        displayText = evaluated.toString().removeSuffix(".0"),
                        formulaText = "$calcAccumulator =",
                        lastResult = evaluated
                    )
                    calcAccumulator = evaluated.toString().removeSuffix(".0")
                } catch (e: Exception) {
                    _calculatorState.value = _calculatorState.value.copy(displayText = "Error")
                    calcAccumulator = ""
                }
            }
            else -> {
                calcAccumulator += key
                _calculatorState.value = _calculatorState.value.copy(
                    displayText = calcAccumulator
                )
            }
        }
    }

    private fun evaluateSimpleMath(expr: String): Double {
        val sanitized = expr.replace("×", "*").replace("÷", "/")
        val parts = sanitized.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        if (parts.size == 1) return parts[0].toDoubleOrNull() ?: 0.0
        var result = parts[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < parts.size - 1) {
            val op = parts[i]
            val next = parts[i + 1].toDoubleOrNull() ?: 0.0
            when (op) {
                "+" -> result += next
                "-" -> result -= next
                "*" -> result *= next
                "/" -> if (next != 0.0) result /= next
            }
            i += 2
        }
        return result
    }

    // --- Media Operations ---

    fun importMedia(uri: Uri, contentResolver: ContentResolver, mediaType: String, album: String) {
        viewModelScope.launch {
            val result = localRepository.importMediaFromUri(
                uri = uri,
                contentResolver = contentResolver,
                mediaType = mediaType,
                albumName = album,
                isDecoy = _isDecoyVault.value
            )
            result.onSuccess {
                _statusMessage.value = "Successfully encrypted and imported ${it.title}"
            }.onFailure {
                _statusMessage.value = "Failed to import: ${it.localizedMessage}"
            }
        }
    }

    fun saveNote(title: String, content: String, album: String = "Notes") {
        viewModelScope.launch {
            val result = localRepository.saveSecretNote(
                title = title,
                content = content,
                albumName = album,
                isDecoy = _isDecoyVault.value
            )
            result.onSuccess {
                _statusMessage.value = "Secret note encrypted and saved"
            }.onFailure {
                _statusMessage.value = "Failed to save note: ${it.localizedMessage}"
            }
        }
    }

    fun selectMedia(entity: VaultMediaEntity?) {
        _selectedMedia.value = entity
        _decryptedBitmap.value = null
        _decryptedNoteContent.value = null
        if (entity != null) {
            viewModelScope.launch {
                if (entity.mediaType == "NOTE") {
                    _decryptedNoteContent.value = localRepository.decryptNote(entity)
                } else if (entity.mediaType == "PHOTO") {
                    _decryptedBitmap.value = localRepository.decryptToBitmap(entity)
                }
            }
        }
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch {
            localRepository.toggleFavorite(id, current)
        }
    }

    fun moveAlbum(id: String, newAlbum: String) {
        viewModelScope.launch {
            localRepository.moveAlbum(id, newAlbum)
            _statusMessage.value = "Moved to $newAlbum"
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            localRepository.moveToTrash(id)
            if (_selectedMedia.value?.id == id) {
                _selectedMedia.value = null
            }
            _statusMessage.value = "Moved to Recycle Bin"
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            localRepository.restoreFromTrash(id)
            _statusMessage.value = "Restored from Recycle Bin"
        }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch {
            localRepository.deletePermanently(id)
            _statusMessage.value = "Permanently deleted"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            localRepository.emptyTrash(_isDecoyVault.value)
            _statusMessage.value = "Recycle bin emptied"
        }
    }

    // --- Cloud Backup & Restore ---

    fun backupAllToCloud() {
        if (_currentUser.value == null) {
            _statusMessage.value = "Please sign in with Google to use cloud backup"
            return
        }
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val items = localRepository.getAllForBackup()
                var successCount = 0
                var totalBytes = 0L

                for (item in items) {
                    val encryptedBytes = localRepository.getEncryptedBytes(item)
                    if (encryptedBytes.isNotEmpty()) {
                        val payloadBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                        val cloudItem = CloudMediaItem(
                            id = item.id,
                            userId = _currentUser.value!!.uid,
                            title = item.title,
                            mediaType = item.mediaType,
                            albumName = item.albumName,
                            originalFileName = item.originalFileName,
                            fileSizeBytes = item.fileSizeBytes,
                            encryptionAlgorithm = "AES-256-GCM",
                            ivBase64 = item.ivBase64,
                            encryptedDataPayload = payloadBase64,
                            checksum = item.checksum,
                            isFavorite = item.isFavorite,
                            isDecoy = false
                        )
                        cloudRepository.backupMediaItem(cloudItem)
                        localRepository.markSynced(item.id)
                        successCount++
                        totalBytes += item.fileSizeBytes
                    }
                }

                cloudRepository.saveVaultSettings(backupEnabled = true, totalSizeBytes = totalBytes)
                _statusMessage.value = "Cloud Backup complete: $successCount items backed up!"
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.localizedMessage}"
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun restoreAllFromCloud() {
        if (_currentUser.value == null) {
            _statusMessage.value = "Please sign in with Google to restore"
            return
        }
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val cloudItems = cloudRepository.fetchCloudItemsOnce()
                var restoredCount = 0

                for (cloudItem in cloudItems) {
                    if (cloudItem.encryptedDataPayload.isNotBlank()) {
                        val payload = Base64.decode(cloudItem.encryptedDataPayload, Base64.NO_WRAP)
                        localRepository.restoreFromCloudItem(cloudItem, payload)
                        restoredCount++
                    }
                }
                _statusMessage.value = "Restored $restoredCount items from secure cloud!"
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.localizedMessage}"
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    private data class CombinedFilter(
        val isDecoy: Boolean,
        val tab: VaultTab,
        val album: String?,
        val query: String
    )
}
