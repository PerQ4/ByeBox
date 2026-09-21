package com.perqa.byebox.ui.main

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> String,
    onReorder: (List<T>) -> Unit,
    cardColor: @Composable (item: T, isDragged: Boolean) -> Color,
    modifier: Modifier = Modifier,
    handleTint: @Composable (item: T, isDragged: Boolean) -> Color = { _, _ -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) },
    itemContent: @Composable RowScope.(item: T, isDragged: Boolean) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val currentItems by rememberUpdatedState(items)
    val currentDraggedIndex by rememberUpdatedState(draggedIndex)
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 86.dp.toPx() }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { key(it) }) { item ->
            val index = items.indexOfFirst { key(it) == key(item) }
            val isDragged = draggedIndex == index
            val currentIndex by rememberUpdatedState(index)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragged) 50f else 1f)
                    .graphicsLayer {
                        if (isDragged) {
                            translationY = dragOffsetY
                            scaleX = 1.03f
                            scaleY = 1.03f
                        } else {
                            translationY = 0f
                            scaleX = 1f
                            scaleY = 1f
                        }
                    }
                    .shadow(
                        elevation = if (isDragged) 16.dp else 2.dp,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor(item, isDragged))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggedIndex = currentIndex
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y

                                        val currIndex = currentDraggedIndex
                                        if (currIndex != null) {
                                            val list = currentItems
                                            if (dragOffsetY > itemHeightPx / 2 && currIndex < list.lastIndex) {
                                                val next = currIndex + 1
                                                val mutable = list.toMutableList()
                                                val moved = mutable.removeAt(currIndex)
                                                mutable.add(next, moved)
                                                onReorder(mutable)
                                                draggedIndex = next
                                                dragOffsetY -= itemHeightPx
                                            } else if (dragOffsetY < -itemHeightPx / 2 && currIndex > 0) {
                                                val prev = currIndex - 1
                                                val mutable = list.toMutableList()
                                                val moved = mutable.removeAt(currIndex)
                                                mutable.add(prev, moved)
                                                onReorder(mutable)
                                                draggedIndex = prev
                                                dragOffsetY += itemHeightPx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffsetY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reorder,
                            contentDescription = null,
                            tint = handleTint(item, isDragged),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    itemContent(item, isDragged)
                }
            }
        }
    }
}
