package com.unistream.webnovel.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unistream.core.ui.theme.NovelTheme
import com.unistream.webnovel.viewmodel.ImportState
import com.unistream.webnovel.viewmodel.NovelViewModel

@Composable
fun NovelImportScreen(
    onBack: () -> Unit,
    onImportSuccess: (Long) -> Unit,
    viewModel: NovelViewModel = hiltViewModel()
) {
    var urlInput by remember { mutableStateOf("") }
    val importState by viewModel.importState.collectAsState()

    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            onImportSuccess((importState as ImportState.Success).novelId)
            viewModel.resetImportState()
        }
    }

    NovelTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Import Novel", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Link,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Paste a novel URL",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // URL Input
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Novel URL") },
                    placeholder = { Text("https://www.webnovel.com/book/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    trailingIcon = {
                        if (urlInput.isNotBlank()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Import button
                Button(
                    onClick = { viewModel.importNovel(urlInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = urlInput.isNotBlank() && importState !is ImportState.Loading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (importState is ImportState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Importing...")
                    } else {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Novel", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Error message
                AnimatedVisibility(visible = importState is ImportState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                (importState as? ImportState.Error)?.message ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Supported sources
                HorizontalDivider()
                Text(
                    "Supported Sources",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                val sources = listOf(
                    "Webnovel.com" to Icons.Default.Web,
                    "RoyalRoad.com" to Icons.Default.AutoStories,
                    "WuxiaWorld.com" to Icons.Default.MenuBook,
                    "Other sites (generic parser)" to Icons.Default.Language
                )

                sources.forEach { (name, icon) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
