package com.example.actitracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actitracker.R
import com.example.actitracker.data.model.ActivityItem
import com.example.actitracker.data.model.TagItem
import com.example.actitracker.ui.components.ActivityRowDimens
import com.example.actitracker.ui.components.AppIcon
import com.example.actitracker.ui.components.CircleIconButton
import com.example.actitracker.ui.components.ReorderableLazyColumn
import com.example.actitracker.ui.components.SwipeableActivityRow
import com.example.actitracker.ui.components.formatSeconds
import com.example.actitracker.ui.components.verticalScrollbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TodayScreen(
    activities: List<ActivityItem>,
    activeActivityId: Long?,
    ticker: Long,
    onStartActivity: (Long) -> Unit,
    onStopActivity: (Long) -> Unit,
    onManageClick: () -> Unit,
    onCreateStart: () -> Unit,
    onCreateDismiss: () -> Unit,
    onCreateSave: (ActivityItem) -> Unit,
    isCreating: Boolean,
    modifier: Modifier = Modifier,
    allTags: List<TagItem> = emptyList(),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    onQuickPanelToggle: (ActivityItem) -> Unit,
    onReorderActivities: (List<ActivityItem>) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Tag Filter State: null = All, -1L = No Tag, else = Tag ID
    var selectedTagFilterId by remember { mutableStateOf<Long?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val isFilterActive = searchQuery.isNotBlank() || selectedTagFilterId != null

    // Logic for filtering activities
    val filteredActivities = remember(
        activities,
        searchQuery,
        selectedTagFilterId
    ) {
        activities.filter { activity ->
            val matchesSearch = searchQuery.isBlank() ||
                    activity.name.contains(searchQuery, ignoreCase = true)

            val matchesTag = when (selectedTagFilterId) {
                null -> true // All tags
                -1L -> activity.tagIds.isEmpty() // No tag
                else -> activity.tagIds.contains(selectedTagFilterId)
            }

            matchesSearch && matchesTag
        }
    }

    // Active activities list for the "Current task" block
    val activeActivities = remember(activities, activeActivityId) {
        activities.filter { it.id == activeActivityId }
    }

    var swipedActivity by remember { mutableStateOf<ActivityItem?>(null) }

    Scaffold(
        containerColor = backgroundColor,
        contentColor = contentColor,
        topBar = {
            TodayTopBar(
                ticker = ticker,
                contentColor = contentColor,
                onManageClick = onManageClick
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            CircleIconButton(
                onClick = onCreateStart,
                painter = painterResource(R.drawable.ic_add_outline),
                outerShape = CircleShape,
                contentDescription = stringResource(R.string.add_activity),
                size = 50.dp,
                containerColor = contentColor,
                iconTint = backgroundColor,
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    clip = false
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 2) Block: Current task
            CurrentTaskBlock(
                activeActivities = activeActivities,
                contentColor = contentColor,
                onStopActivity = onStopActivity
            )

            // 1) Block: Task + Search + Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.task_header),
                    fontSize = ActivityRowDimens.headerFontSize * 0.7,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.7f)
                )

                Spacer(
                    Modifier.width(
                        (ActivityRowDimens.activityRowHorizontalSpacerSize.value * 0.7).dp
                    )
                )

                // Search Block
                SearchBox(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    isActive = isSearchActive,
                    onActiveChange = {
                        isSearchActive = it
                        if (!it) searchQuery = ""
                    },
                    focusRequester = focusRequester,
                    contentColor = contentColor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(
                    Modifier.width(
                        (ActivityRowDimens.activityRowHorizontalSpacerSize.value * 0.7).dp
                    )
                )

                // Filter Zone
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showFilterMenu = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterText = when (selectedTagFilterId) {
                            null -> stringResource(R.string.filter_all_tags)
                            -1L -> stringResource(R.string.filter_no_tag)
                            else -> allTags.find { it.id == selectedTagFilterId }?.name
                                ?: stringResource(R.string.activity_tag_label)
                        }
                        Text(
                            text = filterText,
                            fontSize = ActivityRowDimens.headerFontSize * 0.7,
                            color = contentColor.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_description),
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.background(backgroundColor)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.filter_all_tags),
                                    color = contentColor
                                )
                            },
                            onClick = {
                                selectedTagFilterId = null
                                showFilterMenu = false
                            }
                        )
                        allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name, color = contentColor) },
                                onClick = {
                                    selectedTagFilterId = tag.id
                                    showFilterMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.filter_no_tag),
                                    color = contentColor
                                )
                            },
                            onClick = {
                                selectedTagFilterId = -1L
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Main Activities List
            if (isFilterActive) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState)
                ) {
                    items(filteredActivities, key = { it.id }) { activity ->
                        SwipeableActivityRow(
                            activity = activity,
                            isActive = activity.id == activeActivityId,
                            backgroundColor = backgroundColor,
                            contentColor = contentColor,
                            onClick = {
                                if (activity.id == activeActivityId) {
                                    onStopActivity(activity.id)
                                } else {
                                    onStartActivity(activity.id)
                                }
                            },
                            onSwipe = {
                                swipedActivity = activity
                            }
                        )
                    }
                }
            } else {
                ReorderableLazyColumn(
                    items = activities,
                    itemKey = { it.id },
                    onReorder = onReorderActivities,
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState)
                ) { activity, _ ->
                    SwipeableActivityRow(
                        activity = activity,
                        isActive = activity.id == activeActivityId,
                        backgroundColor = backgroundColor,
                        contentColor = contentColor,
                        onClick = {
                            if (activity.id == activeActivityId) {
                                onStopActivity(activity.id)
                            } else {
                                onStartActivity(activity.id)
                            }
                        },
                        onSwipe = {
                            swipedActivity = activity
                        }
                    )
                }
            }
        }

        // Dialogs
        if (isCreating) {
            val emptyNameError = stringResource(R.string.error_name_empty)
            EditActivityDialog(
                activity = ActivityItem(-1, "", Color.Cyan, "Task"),
                allTags = allTags,
                isCreating = true,
                onDismiss = onCreateDismiss,
                onSave = {
                    if (it.name.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(emptyNameError)
                        }
                    } else {
                        onCreateSave(it)
                        onCreateDismiss()
                    }
                },
                onDelete = {},
                dialogBackgroundColor = contentColor,
                dialogContentColor = backgroundColor,
                quickPanelCount = activities.count { it.showInQuickPanel }
            )
        }

        swipedActivity?.let { activity ->
            val quickPanelCount = activities.count { it.showInQuickPanel }
            val isAdding = !activity.showInQuickPanel

            if (isAdding && quickPanelCount >= 9) {
                AlertDialog(
                    onDismissRequest = { swipedActivity = null },
                    containerColor = contentColor,
                    titleContentColor = backgroundColor,
                    textContentColor = backgroundColor,
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
                            onClick = { swipedActivity = null },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = backgroundColor
                            )
                        ) {
                            Text(stringResource(R.string.ok_button))
                        }
                    }
                )
            } else {
                QuickPanelToggleDialog(
                    activity = activity,
                    onDismiss = { swipedActivity = null },
                    onToggle = { updated ->
                        onQuickPanelToggle(updated)
                        swipedActivity = null
                    },
                    dialogBackgroundColor = contentColor,
                    dialogContentColor = backgroundColor
                )
            }
        }
    }
}

@Composable
private fun TodayTopBar(
    ticker: Long,
    contentColor: Color,
    onManageClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val dateFormatter = remember(locale) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMddEEE")
        SimpleDateFormat(pattern, locale)
    }
    val dateText = remember(ticker, dateFormatter) {
        dateFormatter.format(Date(ticker))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = ActivityRowDimens.activityWholeRowVerticalPadding,
                horizontal = ActivityRowDimens.activityWholeRowHorizontalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onManageClick)
                .padding(ActivityRowDimens.activityWholeRowVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_manage_activities),
                contentDescription = stringResource(R.string.manage_activities_desc),
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.manage_button),
                fontSize = 12.sp,
                color = contentColor
            )
        }

        Text(
            text = dateText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(40.dp)
            .then(
                if (isActive) {
                    Modifier.border(
                        BorderStroke(1.dp, contentColor),
                        RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!isActive) {
                    onActiveChange(true)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isActive) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_hint),
                tint = contentColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = contentColor, fontSize = 16.sp),
                        cursorBrush = SolidColor(contentColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_search),
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            onActiveChange(false)
                        }
                )
            }
        }
    }
}

@Composable
private fun CurrentTaskBlock(
    activeActivities: List<ActivityItem>,
    contentColor: Color,
    onStopActivity: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.current_task_header),
            fontSize = ActivityRowDimens.headerFontSize * 0.7,
            color = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        AnimatedContent(
            targetState = activeActivities,
            label = "CurrentTaskAnimation",
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(300)
                ) togetherWith fadeOut(
                    animationSpec = tween(300)
                )
            }
        ) { currentActiveList ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        ActivityRowDimens.currentTaskPadding * 2 +
                                ActivityRowDimens.touchTargetSize +
                                ActivityRowDimens.currentTaskBorder
                    )
                    .then(
                        if (currentActiveList.isNotEmpty()) {
                            Modifier.border(
                                BorderStroke(
                                    ActivityRowDimens.currentTaskBorder,
                                    contentColor
                                ),
                                RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentActiveList.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_activity_running),
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ActivityRowDimens.currentTaskPadding)
                    ) {
                        currentActiveList.forEach { activity ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    iconName = activity.icon,
                                    tint = activity.color,
                                    modifier = Modifier.size(ActivityRowDimens.ACTIVITY_ROW_ICON_SIZE.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(
                                        (ActivityRowDimens.activityRowHorizontalSpacerSize.value * 0.5)
                                            .dp
                                    )
                                )

                                Text(
                                    text = activity.name,
                                    color = contentColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = ActivityRowDimens.headerFontSize,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(
                                    modifier = Modifier.width(
                                        (ActivityRowDimens.activityRowHorizontalSpacerSize.value * 0.5)
                                            .dp
                                    )
                                )

                                Text(
                                    text = formatSeconds(activity.elapsedSeconds),
                                    color = contentColor,
                                    fontSize = ActivityRowDimens.headerFontSize
                                )

                                Box(
                                    modifier = Modifier
                                        .size(ActivityRowDimens.touchTargetSize)
                                        .clickable { onStopActivity(activity.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_stop_filled),
                                        contentDescription = stringResource(R.string.stop_activity_desc),
                                        tint = contentColor,
                                        modifier = Modifier.size(
                                            ActivityRowDimens.ACTIVITY_ROW_ICON_SIZE.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun TodayScreenPreview() {
    val now = System.currentTimeMillis()

    val activities = listOf(
        ActivityItem(
            id = 1,
            name = "Guitar practice",
            color = Color(0xFF81C784),
            icon = "Music",
            elapsedSeconds = 120
        ),
        ActivityItem(
            id = 2,
            name = "Reading",
            color = Color(0xFF64B5F6),
            icon = "Reading",
            elapsedSeconds = 540
        ),
        ActivityItem(
            id = 3,
            name = "Workout",
            color = Color(0xFFE57373),
            icon = "Exercise",
            elapsedSeconds = 0
        ),
        ActivityItem(
            id = 4,
            name = "Meditation",
            color = Color(0xFFBA68C8),
            icon = "Meditation",
            elapsedSeconds = 0
        )
    )

    TodayScreen(
        activities = activities,
        activeActivityId = 1,
        ticker = now,
        onStartActivity = {},
        onStopActivity = {},
        onManageClick = {},
        onCreateStart = {},
        onCreateDismiss = {},
        onCreateSave = {},
        isCreating = false,
        onQuickPanelToggle = {},
        onReorderActivities = {}
    )
}
