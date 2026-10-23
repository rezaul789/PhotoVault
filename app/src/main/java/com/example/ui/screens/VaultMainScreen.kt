package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VaultMediaEntity
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.viewmodel.VaultTab
import com.example.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isDecoy by viewModel.isDecoyVault.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var showImportMenuDialog by remember { mutableStateOf(false) }

    // Android Zero-Permission Photo Picker for photos and videos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val mediaType = if (mimeType.startsWith("video")) "VIDEO" else "PHOTO"
                viewModel.importMedia(
                    uri = uri,
                    contentResolver = context.contentResolver,
                    mediaType = mediaType,
                    album = selectedAlbum ?: "Default"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search encrypted items...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .testTag("vault_search_field"),
                            singleLine = true
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Vault",
                                tint = if (isDecoy) VaultAmber else VaultCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDecoy) "Decoy Vault" else "Gallery Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isDecoy) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VaultAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("SAFE MODE", color = VaultAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) VaultCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedTab in listOf(VaultTab.PHOTOS, VaultTab.VIDEOS, VaultTab.NOTES)) {
                        IconButton(
                            onClick = { viewModel.toggleGridView() },
                            modifier = Modifier.testTag("grid_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Lock button
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("quick_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = VaultCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultObsidian)
            )
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = VaultSurfaceCard,
                contentColor = VaultCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = VaultCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == VaultTab.PHOTOS,
                    onClick = { viewModel.setSelectedTab(VaultTab.PHOTOS) },
                    icon = { Icon(Icons.Default.Image, contentDescription = "Photos") },
                    text = { Text("Photos", fontSize = 11.sp) },
                    selectedContentColor = VaultCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == VaultTab.VIDEOS,
                    onClick = { viewModel.setSelectedTab(VaultTab.VIDEOS) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Videos") },
                    text = { Text("Videos", fontSize = 11.sp) },
                    selectedContentColor = VaultCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == VaultTab.NOTES,
                    onClick = { viewModel.setSelectedTab(VaultTab.NOTES) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    text = { Text("Notes", fontSize = 11.sp) },
                    selectedContentColor = VaultCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == VaultTab.CLOUD,
                    onClick = { viewModel.setSelectedTab(VaultTab.CLOUD) },
                    icon = { Icon(Icons.Default.CloudDone, contentDescription = "Cloud Backup") },
                    text = { Text("Cloud", fontSize = 11.sp) },
                    selectedContentColor = VaultCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedTab == VaultTab.SETTINGS,
                    onClick = { viewModel.setSelectedTab(VaultTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    text = { Text("Settings", fontSize = 11.sp) },
                    selectedContentColor = VaultCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            if (selectedTab in listOf(VaultTab.PHOTOS, VaultTab.VIDEOS, VaultTab.NOTES)) {
                FloatingActionButton(
                    onClick = { showImportMenuDialog = true },
                    containerColor = VaultCyan,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("fab_add_media")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Encrypted Item")
                }
            }
        },
        containerColor = VaultObsidian
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                VaultTab.PHOTOS, VaultTab.VIDEOS, VaultTab.NOTES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Album & Filter Chips Row
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedAlbum == null,
                                    onClick = { viewModel.setSelectedAlbum(null) },
                                    label = { Text("All") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VaultCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                            items(albums) { album ->
                                FilterChip(
                                    selected = selectedAlbum == album,
                                    onClick = { viewModel.setSelectedAlbum(album) },
                                    label = { Text(album) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VaultCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { showNewAlbumDialog = true },
                                    label = { Text("+ Album") },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }

                        // Content Items
                        if (mediaItems.isEmpty()) {
                            EmptyVaultView(
                                tab = selectedTab,
                                onImport = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                onCreateNote = { showNewNoteDialog = true }
                            )
                        } else {
                            if (isGridView && selectedTab != VaultTab.NOTES) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 110.dp),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(mediaItems, key = { it.id }) { item ->
                                        MediaGridCard(
                                            item = item,
                                            viewModel = viewModel,
                                            onClick = { viewModel.selectMedia(item) }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(mediaItems, key = { it.id }) { item ->
                                        MediaListCard(
                                            item = item,
                                            onClick = { viewModel.selectMedia(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                VaultTab.CLOUD -> {
                    CloudBackupScreen(viewModel = viewModel)
                }
                VaultTab.SETTINGS -> {
                    VaultSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Import Options Dialog
    if (showImportMenuDialog) {
        AlertDialog(
            onDismissRequest = { showImportMenuDialog = false },
            title = { Text("Secure & Encrypt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportMenuDialog = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Import", tint = VaultCyan)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Import Photos / Videos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Text("Zero-permission device picker", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportMenuDialog = false
                                showNewNoteDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = VaultSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Note", tint = VaultPurple)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("New Secret Note", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Text("AES-256 encrypted confidential note", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportMenuDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Secret Note Dialog
    if (showNewNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewNoteDialog = false },
            title = { Text("Create Secret Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Secret Content (Encrypted)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            viewModel.saveNote(noteTitle, noteContent, selectedAlbum ?: "Notes")
                            showNewNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                ) {
                    Text("Encrypt & Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Album Dialog
    if (showNewAlbumDialog) {
        var albumNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewAlbumDialog = false },
            title = { Text("New Folder / Album") },
            text = {
                OutlinedTextField(
                    value = albumNameInput,
                    onValueChange = { albumNameInput = it },
                    label = { Text("Album Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (albumNameInput.isNotBlank()) {
                            viewModel.setSelectedAlbum(albumNameInput.trim())
                            showNewAlbumDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MediaGridCard(
    item: VaultMediaEntity,
    viewModel: VaultViewModel,
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        if (item.mediaType == "PHOTO") {
            thumbnailBitmap = viewModel.localRepository.decryptToBitmap(item)
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("media_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.mediaType) {
                            "VIDEO" -> Icons.Default.Videocam
                            "NOTE" -> Icons.Default.Description
                            else -> Icons.Default.Image
                        },
                        contentDescription = item.mediaType,
                        tint = VaultCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Sync Status Badge on top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.cloudSyncStatus == "SYNCED") Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = "Sync Status",
                    tint = if (item.cloudSyncStatus == "SYNCED") VaultEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Favorite star on top-left if favorite
            if (item.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = VaultCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Bottom title overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MediaListCard(
    item: VaultMediaEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("media_list_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VaultSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.mediaType) {
                        "VIDEO" -> Icons.Default.Videocam
                        "NOTE" -> Icons.Default.Description
                        else -> Icons.Default.Image
                    },
                    contentDescription = item.mediaType,
                    tint = VaultCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.albumName} • ${item.fileSizeBytes / 1024} KB • AES-256",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = if (item.cloudSyncStatus == "SYNCED") Icons.Default.CloudDone else Icons.Default.CloudQueue,
                contentDescription = "Sync Status",
                tint = if (item.cloudSyncStatus == "SYNCED") VaultEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EmptyVaultView(
    tab: VaultTab,
    onImport: () -> Unit,
    onCreateNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VaultCyan.copy(alpha = 0.1f))
                .border(1.dp, VaultCyan.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (tab) {
                    VaultTab.NOTES -> Icons.Default.Description
                    VaultTab.VIDEOS -> Icons.Default.Videocam
                    else -> Icons.Default.Image
                },
                contentDescription = "Empty Vault",
                tint = VaultCyan,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = when (tab) {
                VaultTab.NOTES -> "No Secret Notes Yet"
                VaultTab.VIDEOS -> "No Encrypted Videos"
                else -> "Vault Is Empty"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import files from your device to encrypt them with military-grade AES-256-GCM. Decrypted only in memory.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (tab == VaultTab.NOTES) {
            Button(
                onClick = onCreateNote,
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Secret Note", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Import & Encrypt Media", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
