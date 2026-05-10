package com.example.actitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.actitracker.R
import com.example.actitracker.data.model.ActivityItem
import com.example.actitracker.data.model.TagItem
import com.example.actitracker.ui.components.IconMapper
import com.example.actitracker.ui.theme.ActitrackerTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditActivityDialog(
    activity: ActivityItem,
    allTags: List<TagItem>,
    onDismiss: () -> Unit,
    onSave: (ActivityItem) -> Unit,
    onDelete: () -> Unit,
    isCreating: Boolean = false,
    dialogBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    dialogContentColor: Color = MaterialTheme.colorScheme.onSurface,
    quickPanelCount: Int = 0
) {
    var name by remember { mutableStateOf(activity.name) }
    var selectedColor by remember { mutableStateOf(activity.color) }
    var selectedIconName by remember { mutableStateOf(activity.icon) }
    var showInQuickPanel by remember { mutableStateOf(activity.showInQuickPanel) }
    var selectedTagIds by remember { mutableStateOf(activity.tagIds) }
    var showTagMenu by remember { mutableStateOf(false) }

    // State for showing selection dialogs
    val showColorPicker = remember { mutableStateOf(false) }
    val showIconPicker = remember { mutableStateOf(false) }
    val showLimitWarning = remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dummyFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackgroundColor,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        iconContentColor = dialogContentColor,

        title = {
            Text(
                if (isCreating) stringResource(R.string.create_activity_title) else stringResource(
                    R.string.edit_activity_title
                )
            )
        },

        text = {
            Column {
                Box(
                    modifier = Modifier
                        .size(0.dp)
                        .focusRequester(dummyFocusRequester)
                        .focusable()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            stringResource(R.string.activity_name_label),
                            color = dialogContentColor.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogContentColor,
                        unfocusedTextColor = dialogContentColor,
                        focusedBorderColor = dialogContentColor,
                        unfocusedBorderColor = dialogContentColor.copy(alpha = 0.5f),
                        cursorColor = dialogContentColor,
                        errorBorderColor = Color.Red,
                        errorTextColor = dialogContentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color selection button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showColorPicker.value = true
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.activity_color_label), color = dialogContentColor)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(
                                1.dp,
                                dialogContentColor.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }

                // Icon selection button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showIconPicker.value = true
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.activity_icon_label), color = dialogContentColor)
                    Icon(
                        imageVector = IconMapper.getIcon(selectedIconName),
                        contentDescription = selectedIconName,
                        tint = dialogContentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tags section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.activity_tag_label),
                        fontSize = 16.sp,
                        color = dialogContentColor,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box {
                            TextButton(
                                onClick = { showTagMenu = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = dialogContentColor
                                )
                            ) {
                                Text(stringResource(R.string.add_tag_button))
                            }
                            DropdownMenu(
                                expanded = showTagMenu,
                                onDismissRequest = { showTagMenu = false },
                                containerColor = dialogBackgroundColor
                            ) {
                                val unselectedTags = allTags.filter { it.id !in selectedTagIds }
                                if (unselectedTags.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(R.string.no_more_tags),
                                                color = dialogContentColor.copy(alpha = 0.5f)
                                            )
                                        },
                                        onClick = { showTagMenu = false }
                                    )
                                } else {
                                    unselectedTags.forEach { tag ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Label,
                                                        contentDescription = null,
                                                        tint = tag.color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(tag.name, color = dialogContentColor)
                                                }
                                            },
                                            onClick = {
                                                selectedTagIds = selectedTagIds + tag.id
                                                showTagMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedTagIds.forEach { tagId ->
                                val tag = allTags.find { it.id == tagId }
                                if (tag != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .border(
                                                1.dp,
                                                dialogContentColor.copy(alpha = 0.3f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 2.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Label,
                                                contentDescription = null,
                                                tint = tag.color,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = tag.name,
                                                fontSize = 12.sp,
                                                color = dialogContentColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.remove_tag_desc),
                                                tint = dialogContentColor,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable {
                                                        selectedTagIds = selectedTagIds - tagId
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.show_in_notification_label),
                            fontSize = 14.sp,
                            color = dialogContentColor
                        )
                        Text(
                            text = stringResource(R.string.show_in_notification_desc),
                            fontSize = 11.sp,
                            color = dialogContentColor.copy(alpha = 0.6f),
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = showInQuickPanel,
                        onCheckedChange = {
                            if (it && !showInQuickPanel && quickPanelCount >= 9) {
                                showLimitWarning.value = true
                            } else {
                                showInQuickPanel = it
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = dialogBackgroundColor,
                            checkedTrackColor = dialogContentColor,
                            uncheckedThumbColor = dialogContentColor.copy(alpha = 0.5f),
                            uncheckedTrackColor = dialogContentColor.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            activity.copy(
                                name = name.trim(),
                                color = selectedColor,
                                icon = selectedIconName,
                                showInQuickPanel = showInQuickPanel,
                                tagIds = selectedTagIds
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dialogContentColor,
                    contentColor = dialogBackgroundColor
                )
            ) {
                Text(stringResource(R.string.save_button))
            }
        },

        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = dialogContentColor
                    )
                ) {
                    Text(stringResource(R.string.cancel_button))
                }

                if (!isCreating) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text(stringResource(R.string.delete_button))
                    }
                }
            }
        }
    )

    // Color selection dialog
    if (showColorPicker.value) {
        Dialog(
            onDismissRequest = { showColorPicker.value = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ColorPickerScreen(
                    initialColor = selectedColor,
                    onColorConfirmed = {
                        selectedColor = it
                        showColorPicker.value = false
                    },
                    onDismiss = { showColorPicker.value = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .wrapContentHeight(),
                    backgroundColor = dialogBackgroundColor,
                    contentColor = dialogContentColor
                )
            }
        }
    }

    // Icon selection dialog
    if (showIconPicker.value) {
        Dialog(
            onDismissRequest = { showIconPicker.value = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    IconPickerScreen(
                        initialIconName = selectedIconName,
                        onIconSelected = {
                            selectedIconName = it
                            showIconPicker.value = false
                        },
                        onDismiss = { showIconPicker.value = false }
                    )
                }
            }
        }
    }

    if (showLimitWarning.value) {
        AlertDialog(
            onDismissRequest = { showLimitWarning.value = false },
            containerColor = dialogContentColor,
            titleContentColor = dialogBackgroundColor,
            textContentColor = dialogBackgroundColor,
            title = {
                Text(
                    text = stringResource(R.string.quick_panel_limit_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.quick_panel_limit_message))
            },
            confirmButton = {
                TextButton(
                    onClick = { showLimitWarning.value = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = dialogBackgroundColor
                    )
                ) {
                    Text(stringResource(R.string.ok_button))
                }
            }
        )
    }

    val isInspect = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspect) {
            dummyFocusRequester.requestFocus()
        }
    }
}

@Preview(showBackground = true, apiLevel = 35)
@Composable
fun EditActivityDialogPreview() {
    val sampleActivity = ActivityItem(
        id = 1L,
        name = "Running",
        icon = "Exercise",
        color = Color(0xFF6200EE),
        elapsedSeconds = 754,
        firstStartDayTime = System.currentTimeMillis() - 3_600_000,
        tagIds = listOf(1L, 2L)
    )

    val sampleTags = listOf(
        TagItem(id = 1L, name = "Sport", color = Color(0xFF4CAF50)),
        TagItem(id = 2L, name = "Health", color = Color(0xFFFF9800)),
        TagItem(id = 3L, name = "Work", color = Color(0xFF2196F3))
    )

    ActitrackerTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EditActivityDialog(
                activity = sampleActivity,
                allTags = sampleTags,
                onDismiss = {},
                onSave = {},
                onDelete = {},
                isCreating = false
            )
        }
    }
}
