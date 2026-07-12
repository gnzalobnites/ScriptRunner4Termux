package io.github.swiftstagrime.termuxrunner.ui.features.editor
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R

@Composable
fun CodePageTabs(
    pageCount: Int,
    pageNames: List<String>,
    currentPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onRenamePage: (Int, String) -> Unit,
    onReorderPage: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val shouldScroll by remember {
        derivedStateOf {
            currentPageIndex >= (
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            )
        }
    }

    var renameDialogIndex by remember { mutableStateOf<Int?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dropTargetIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(renameDialogIndex) {
        renameDialogIndex?.let { index ->
            renameDraft = pageNames.getOrNull(index) ?: ""
        }
    }

    if (shouldScroll && dragIndex == -1) {
        LaunchedEffect(currentPageIndex) {
            listState.animateScrollToItem(currentPageIndex)
        }
    }

    Row(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(pageCount) { index ->
                val pageName =
                    pageNames.getOrNull(index)?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.cd_default_page_name, index + 1)
                CodePageTab(
                    label = pageName,
                    isSelected = index == currentPageIndex,
                    isDragging = dragIndex == index,
                    isDropTarget = dropTargetIndex == index && dragIndex != index,
                    dragOffsetX = if (dragIndex == index) dragOffsetX else 0f,
                    onClick = {
                        if (dragIndex == -1) {
                            onPageSelected(index)
                        }
                    },
                    onDelete = { onDeletePage(index) },
                    onRename = {
                        if (dragIndex == -1) {
                            renameDialogIndex = index
                        }
                    },
                    onStartDrag = {
                        dragIndex = index
                        dragOffsetX = 0f
                        dropTargetIndex = -1
                    },
                    onDrag = { dragAmount ->
                        if (dragIndex == index) {
                            dragOffsetX += dragAmount
                            dropTargetIndex =
                                if (dragOffsetX > 60f && index < pageCount - 1) {
                                    index + 1
                                } else if (dragOffsetX < -60f && index > 0) {
                                    index - 1
                                } else {
                                    -1
                                }
                        }
                    },
                    onDragEnd = {
                        if (dragIndex == index && dropTargetIndex >= 0) {
                            onReorderPage(dragIndex, dropTargetIndex)
                        }
                        dragIndex = -1
                        dragOffsetX = 0f
                        dropTargetIndex = -1
                    },
                    canDelete = pageCount > 1,
                )
            }
        }

        TextButton(
            onClick = onAddPage,
            modifier = Modifier.height(32.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_page),
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.cd_add_page),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    renameDialogIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { renameDialogIndex = null },
            title = { Text(stringResource(R.string.cd_rename_page)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text(stringResource(R.string.cd_page_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenamePage(index, renameDraft)
                        renameDialogIndex = null
                    },
                ) {
                    Text(stringResource(R.string.cd_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogIndex = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun CodePageTab(
    label: String,
    isSelected: Boolean,
    isDragging: Boolean,
    isDropTarget: Boolean,
    dragOffsetX: Float,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onStartDrag: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (isDropTarget) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        } else if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }

    val textColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val elevation by animateFloatAsState(targetValue = if (isDragging) 8f else 0f)
    val alpha by animateFloatAsState(targetValue = if (isDragging) 0.85f else 1f)

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    shadowElevation = elevation
                    this.alpha = alpha
                    translationX = dragOffsetX
                }.background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                ).pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onStartDrag() },
                        onDrag = { _, dragAmount ->
                            onDrag(dragAmount.x)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                    )
                }.pointerInput(Unit) {
                    if (!isDragging) {
                        detectTapGestures(
                            onTap = { onClick() },
                        )
                    }
                }.padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 120.dp),
            )
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(16.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cd_rename_page),
                    modifier = Modifier.size(12.dp),
                    tint = textColor.copy(alpha = 0.6f),
                )
            }
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(16.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_delete_page),
                        modifier = Modifier.size(12.dp),
                        tint = textColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
