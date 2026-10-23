package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.VaultMainScreen
import com.example.ui.theme.GalleryVaultTheme
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryVaultTheme {
                VaultAppRoot()
            }
        }
    }
}

@Composable
fun VaultAppRoot(viewModel: VaultViewModel = viewModel()) {
    val isLocked by viewModel.isLocked.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            snackbarHostState.showSnackbar(
                message = statusMessage ?: "",
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLocked -> {
                    LockScreen(viewModel = viewModel)
                }
                selectedMedia != null -> {
                    MediaViewerScreen(
                        entity = selectedMedia!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectMedia(null) }
                    )
                }
                else -> {
                    VaultMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
