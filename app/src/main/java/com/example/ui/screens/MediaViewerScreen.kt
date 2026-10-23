package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VaultMediaEntity
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultObsidian
import com.example.ui.theme.VaultPurple
import com.example.ui.theme.VaultSurfaceCard
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    entity: VaultMediaEntity,
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val decryptedBitmap by viewModel.decryptedBitmap.collectAsState()
    val decryptedNote by viewModel.decryptedNoteContent.collectAsState()
    val albums by viewModel.albums.collectAsState()

    var showMoveAlbumDialog by remember { mutableStateOf(false) }
    var selectedNewAlbum by remember { mutableStateOf(entity.albumName) }
    var customAlbumText by remember { mutableStateOf("") }

    var isEditingNote by remember { mutableStateOf(false) }
    var editedNoteContent by remember { mutableStateOf(decryptedNote ?: "") }

    val formattedDate = remember(entity.createdAt) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(entity.createdAt))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = entity.title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AES-256-GCM Encrypted • ${entity.albumName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VaultCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Favorite Toggle
                    IconButton(
                        onClick = { viewModel.toggleFavorite(entity.id, entity.isFavorite) },
                        modifier = Modifier.testTag("viewer_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (entity.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (entity.isFavorite) VaultCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Move Album
                    IconButton(
                        onClick = { showMoveAlbumDialog = true },
                        modifier = Modifier.testTag("viewer_move_album_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Move Album",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete / Move to Trash
                    IconButton(
                        onClick = {
                            viewModel.moveToTrash(entity.id)
                            onBack()
                        },
                        modifier = Modifier.testTag("viewer_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Move to Trash",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultObsidian)
            )
        },
        containerColor = VaultObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (entity.mediaType) {
                "PHOTO" -> {
                    if (decryptedBitmap != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = decryptedBitmap!!.asImageBitmap(),
                                    contentDescription = entity.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            MediaMetadataFooter(entity, formattedDate)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VaultCyan)
                        }
                    }
                }
                "VIDEO" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(VaultSurfaceCard)
                                .border(1.dp, VaultBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play Video",
                                    tint = VaultCyan,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = entity.originalFileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Secure In-Memory Video Container",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        MediaMetadataFooter(entity, formattedDate)
                    }
                }
                "NOTE" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encrypted Note",
                                            tint = VaultCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Encrypted Secret Note",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (isEditingNote) {
                                                viewModel.saveNote(entity.title, editedNoteContent, entity.albumName)
                                                isEditingNote = false
                                            } else {
                                                editedNoteContent = decryptedNote ?: ""
                                                isEditingNote = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isEditingNote) Icons.Default.Save else Icons.Default.Edit,
                                            contentDescription = if (isEditingNote) "Save" else "Edit",
                                            tint = VaultCyan
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isEditingNote) {
                                    OutlinedTextField(
                                        value = editedNoteContent,
                                        onValueChange = { editedNoteContent = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                        placeholder = { Text("Write your encrypted secret note...") }
                                    )
                                } else {
                                    Text(
                                        text = decryptedNote ?: "Decrypting note...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        MediaMetadataFooter(entity, formattedDate)
                    }
                }
            }
        }
    }

    // Move Album Dialog
    if (showMoveAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showMoveAlbumDialog = false },
            title = { Text("Move to Album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val albumOptions = (listOf("Default", "Personal", "Work", "Private") + albums).distinct()
                    albumOptions.forEach { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedNewAlbum == album) VaultCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = album,
                                color = if (selectedNewAlbum == album) VaultCyan else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("album_option_$album")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveAlbum(entity.id, selectedNewAlbum)
                        showMoveAlbumDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                ) {
                    Text("Move", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MediaMetadataFooter(entity: VaultMediaEntity, formattedDate: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Encryption:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("AES-256-GCM / Hardware Keystore", fontSize = 11.sp, color = VaultCyan, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Size:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${entity.fileSizeBytes / 1024} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Secured on:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
