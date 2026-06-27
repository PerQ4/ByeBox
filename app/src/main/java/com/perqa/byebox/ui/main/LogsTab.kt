package com.perqa.byebox.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogsTab(
    state: MainUiState,
    viewModel: MainScreenViewModel,
    embedMode: Boolean = false
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val filteredLogs = remember(searchQuery, state.logs) {
        if (searchQuery.isBlank()) {
            state.logs
        } else {
            state.logs.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (!embedMode) Modifier.padding(horizontal = 8.dp) else Modifier)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Loc.get("search_logs", state.language)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                maxLines = 1,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.exportLogs(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Text(Loc.get("logs_export", state.language), fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = { viewModel.clearLogs() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .weight(1f)
                ) {
                    Text(Loc.get("logs_clear", state.language), fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Terminal View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f))
                .padding(14.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                Text(
                    text = Loc.get("logs_empty", state.language),
                    color = androidx.compose.ui.graphics.Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredLogs,
                        contentType = { "log" }
                    ) { log ->
                        val textColor = when {
                            log.contains("[ERROR]", ignoreCase = true) -> androidx.compose.ui.graphics.Color(0xFFEF4444)
                            log.contains("[WARNING]", ignoreCase = true) -> androidx.compose.ui.graphics.Color(0xFFFBBF24)
                            log.contains("[INFO]", ignoreCase = true) -> androidx.compose.ui.graphics.Color(0xFF60A5FA)
                            log.contains("[DEBUG]", ignoreCase = true) -> androidx.compose.ui.graphics.Color(0xFF10B981)
                            else -> androidx.compose.ui.graphics.Color(0xFFE2E8F0)
                        }
                        Text(
                            text = log,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(if (embedMode) 30.dp else 130.dp)
        )
    }
}
