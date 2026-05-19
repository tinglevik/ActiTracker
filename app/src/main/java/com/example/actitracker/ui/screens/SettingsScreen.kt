package com.example.actitracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.actitracker.R
import com.example.actitracker.ui.components.ContrastUtils
import com.example.actitracker.ui.theme.ActitrackerTheme
import com.example.actitracker.viewmodel.SettingsViewModel
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private enum class ColorPickerTarget { BACKGROUND, TEXT }

data class BackupOptions(
    val activities: Boolean = true,
    val tags: Boolean = true,
    val goals: Boolean = true,
    val logs: Boolean = true,
    val settings: Boolean = true
)

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateToLicenses: () -> Unit,
    contentColor: Color = Color.Black,
    shouldHighlight: Boolean = false
) {
    val backgroundColorState by settingsViewModel.backgroundColor.collectAsState()
    val savedContentColor by settingsViewModel.contentColor.collectAsState()

    LaunchedEffect(shouldHighlight) {
        if (shouldHighlight) {
            settingsViewModel.triggerHighlight()
        }
    }

    SettingsScreenContent(
        settingsViewModel = settingsViewModel,
        backgroundColorState = backgroundColorState,
        savedContentColor = savedContentColor,
        onBackgroundColorChange = { settingsViewModel.saveBackgroundColor(it) },
        onContentColorChange = { settingsViewModel.saveContentColor(it) },
        onShowWarning = { bg, txt ->
            settingsViewModel.showWarning(bg, txt)
        },
        onNavigateToLicenses = onNavigateToLicenses,
        contentColor = contentColor
    )
}

@Composable
fun SettingsScreenContent(
    settingsViewModel: SettingsViewModel,
    backgroundColorState: Color,
    savedContentColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    onContentColorChange: (Color) -> Unit,
    onShowWarning: (Color, Color) -> Unit,
    onNavigateToLicenses: () -> Unit,
    contentColor: Color = Color.Black
) {
    var colorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var showContrastDialog by remember { mutableStateOf(false) }
    var pendingColor by remember { mutableStateOf<Color?>(null) }
    var contrastDialogSource by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var openedFromContrastDialog by remember { mutableStateOf(false) }
    var colorBeforeContrastFlow by remember { mutableStateOf<Color?>(null) }

    var showImportExportDialog by remember { mutableStateOf(false) }
    var showImportFlow by remember { mutableStateOf(false) }
    var importIsConfirmStep by remember { mutableStateOf(false) }
    
    var showClearDataFlow by remember { mutableStateOf(false) }
    var clearDataIsConfirmStep by remember { mutableStateOf(false) }

    var importJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingImportOptions by remember { mutableStateOf<BackupOptions?>(null) }
    var pendingClearOptions by remember { mutableStateOf<BackupOptions?>(null) }
    var exportOptions by remember { mutableStateOf<BackupOptions?>(null) }
    val context = LocalContext.current
    val exportSuccessMessage = stringResource(R.string.export_success)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            exportOptions?.let { options ->
                settingsViewModel.createBackup(
                    activities = options.activities,
                    tags = options.tags,
                    goals = options.goals,
                    logs = options.logs,
                    settings = options.settings
                ) { json ->
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(json)
                            }
                        }
                        settingsViewModel.showSnackbar(exportSuccessMessage)
                    } catch (e: Exception) {
                        settingsViewModel.showSnackbar("Export failed: ${e.message}")
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    importJsonContent = InputStreamReader(inputStream).readText()
                    importIsConfirmStep = false
                    showImportFlow = true
                }
            } catch (e: Exception) {
                settingsViewModel.showSnackbar("Import failed: ${e.message}")
            }
        }
    }

    val onColorConfirmedInternal: (Color) -> Unit = { color ->
        val target = colorPickerTarget
        colorPickerTarget = null
        openedFromContrastDialog = false
        colorBeforeContrastFlow = null
        when (target) {
            ColorPickerTarget.BACKGROUND -> {
                if (!ContrastUtils.isReadable(savedContentColor, color)) {
                    pendingColor = color
                    contrastDialogSource = ColorPickerTarget.BACKGROUND
                    showContrastDialog = true
                } else {
                    onBackgroundColorChange(color)
                }
            }

            ColorPickerTarget.TEXT -> {
                if (!ContrastUtils.isReadable(color, backgroundColorState)) {
                    pendingColor = color
                    contrastDialogSource = ColorPickerTarget.TEXT
                    showContrastDialog = true
                } else {
                    onContentColorChange(color)
                }
            }

            null -> {}
        }
    }

    val onDismissInternal: () -> Unit = {
        if (openedFromContrastDialog && colorBeforeContrastFlow != null) {
            when (contrastDialogSource) {
                ColorPickerTarget.BACKGROUND ->
                    onBackgroundColorChange(colorBeforeContrastFlow!!)

                ColorPickerTarget.TEXT ->
                    onContentColorChange(colorBeforeContrastFlow!!)

                null -> {}
            }
        }
        colorPickerTarget = null
        openedFromContrastDialog = false
        colorBeforeContrastFlow = null
        contrastDialogSource = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by settingsViewModel.snackbarMessage.collectAsState()
    val dimScreen by settingsViewModel.dimScreen.collectAsState()

    Scaffold(
        containerColor = backgroundColorState,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let {
                snackbarHostState.showSnackbar(it)
                settingsViewModel.clearSnackbar()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (dimScreen) 0.5f else 1.0f)
        ) {
            when {
                colorPickerTarget != null && !openedFromContrastDialog -> {
                    val isBackground = colorPickerTarget == ColorPickerTarget.BACKGROUND
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        ColorPickerScreen(
                            modifier = Modifier
                                .fillMaxSize()
//                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .imePadding(),
                            initialColor = if (isBackground)
                                backgroundColorState else savedContentColor,
                            contrastWarning = if (isBackground)
                                stringResource(R.string.contrast_warning_background)
                            else
                                stringResource(R.string.contrast_warning_text),
                            onColorConfirmed = onColorConfirmedInternal,
                            onDismiss = onDismissInternal,
                            backgroundColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Text(
                            text = stringResource(R.string.settings_appearance),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SettingsColorCard(
                            title = stringResource(R.string.settings_background_color),
                            subtitle = stringResource(R.string.settings_background_color_desc),
                            color = backgroundColorState,
                            onClick = {
                                colorPickerTarget = ColorPickerTarget.BACKGROUND
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsColorCard(
                            title = stringResource(R.string.settings_content_color),
                            subtitle = stringResource(R.string.settings_content_color_desc),
                            color = savedContentColor,
                            onClick = {
                                colorPickerTarget = ColorPickerTarget.TEXT
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.settings_data_management),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SettingsActionCard(
                            title = stringResource(R.string.settings_import_export),
                            subtitle = stringResource(R.string.settings_import_export_desc),
                            icon = Icons.Default.SaveAlt,
                            onClick = { showImportExportDialog = true },
                            highlight = settingsViewModel
                                .highlightDataManagement.collectAsState()
                                .value
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsActionCard(
                            title = stringResource(R.string.settings_clear_data),
                            subtitle = stringResource(R.string.settings_clear_data_desc),
                            icon = Icons.Default.Delete,
                            onClick = { 
                                clearDataIsConfirmStep = false
                                showClearDataFlow = true 
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.settings_about),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SettingsActionCard(
                            title = stringResource(R.string.settings_licenses),
                            subtitle = stringResource(R.string.settings_licenses_desc),
                            icon = Icons.Default.Description,
                            onClick = onNavigateToLicenses
                        )
                    }
                }
            }

            if (showImportExportDialog) {
                ImportExportDialog(
                    onDismiss = { showImportExportDialog = false },
                    onExport = { activities, tags, goals, logs, settings ->
                        exportOptions = BackupOptions(activities, tags, goals, logs, settings)
                        exportLauncher.launch("actitracker_backup.json")
                    },
                    onImport = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "*/*")
                        )
                    }
                )
            }

            if (showImportFlow && importJsonContent != null) {
                ImportFlowContainer(
                    isConfirmStep = importIsConfirmStep,
                    pendingOptions = pendingImportOptions,
                    onDismiss = {
                        showImportFlow = false
                        importJsonContent = null
                    },
                    onGoToConfirm = { options ->
                        pendingImportOptions = options
                        importIsConfirmStep = true
                    },
                    onFinalConfirm = {
                        settingsViewModel.restoreBackup(
                            json = importJsonContent!!,
                            activities = pendingImportOptions!!.activities,
                            tags = pendingImportOptions!!.tags,
                            goals = pendingImportOptions!!.goals,
                            logs = pendingImportOptions!!.logs,
                            settings = pendingImportOptions!!.settings,
                            context = context
                        )
                        showImportFlow = false
                        showImportExportDialog = false
                        importJsonContent = null
                        pendingImportOptions = null
                    }
                )
            }

            if (showClearDataFlow) {
                ClearDataFlowContainer(
                    isConfirmStep = clearDataIsConfirmStep,
                    pendingOptions = pendingClearOptions,
                    onDismiss = { showClearDataFlow = false },
                    onGoToConfirm = { options ->
                        pendingClearOptions = options
                        clearDataIsConfirmStep = true
                    },
                    onFinalConfirm = {
                        settingsViewModel.clearData(
                            activities = pendingClearOptions!!.activities,
                            tags = pendingClearOptions!!.tags,
                            goals = pendingClearOptions!!.goals,
                            logs = pendingClearOptions!!.logs,
                            settings = pendingClearOptions!!.settings,
                            context = context
                        )
                        showClearDataFlow = false
                        pendingClearOptions = null
                    }
                )
            }

            if (colorPickerTarget != null && openedFromContrastDialog) {
                val isBackground = colorPickerTarget == ColorPickerTarget.BACKGROUND
                Dialog(
                    onDismissRequest = onDismissInternal,
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
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp))
                                .wrapContentHeight(),
                            initialColor = if (isBackground)
                                backgroundColorState else savedContentColor,
                            contrastWarning = if (isBackground)
                                stringResource(R.string.contrast_warning_background)
                            else
                                stringResource(R.string.contrast_warning_text),
                            onColorConfirmed = onColorConfirmedInternal,
                            onDismiss = onDismissInternal,
                            backgroundColor = backgroundColorState,
                            contentColor = savedContentColor
                        )
                    }
                }
            }

            if (showContrastDialog && pendingColor != null && contrastDialogSource != null) {
                val isFromBg = contrastDialogSource == ColorPickerTarget.BACKGROUND
                ContrastSuggestionDialog(
                    backgroundColor = if (isFromBg) pendingColor!! else backgroundColorState,
                    textColor = if (isFromBg) savedContentColor else pendingColor!!,
                    isBackgroundChange = isFromBg,
                    suggestions = if (isFromBg)
                        ContrastUtils.suggestTextColors(pendingColor!!)
                    else
                        ContrastUtils.suggestBackgroundColors(pendingColor!!),
                    onSuggestionSelected = { suggested ->
                        if (isFromBg) {
                            onBackgroundColorChange(pendingColor!!)
                            onContentColorChange(suggested)
                        } else {
                            onContentColorChange(pendingColor!!)
                            onBackgroundColorChange(suggested)
                        }
                        showContrastDialog = false
                        pendingColor = null
                        contrastDialogSource = null
                    },
                    onOpenColorPicker = {
                        colorBeforeContrastFlow =
                            if (isFromBg) backgroundColorState else savedContentColor
                        openedFromContrastDialog = true
                        if (isFromBg) {
                            onBackgroundColorChange(pendingColor!!)
                            colorPickerTarget = ColorPickerTarget.TEXT
                        } else {
                            onContentColorChange(pendingColor!!)
                            colorPickerTarget = ColorPickerTarget.BACKGROUND
                        }
                        showContrastDialog = false
                        pendingColor = null
                    },
                    onKeepAnyway = {
                        if (isFromBg) {
                            onBackgroundColorChange(pendingColor!!)
                        } else {
                            onContentColorChange(pendingColor!!)
                        }
                        onShowWarning(backgroundColorState, savedContentColor)
                        showContrastDialog = false
                        pendingColor = null
                        contrastDialogSource = null
                    },
                    onDismiss = {
                        showContrastDialog = false
                        pendingColor = null
                        contrastDialogSource = null
                    }
                )
            }
        }
    }
}

@Composable
fun ImportFlowContainer(
    isConfirmStep: Boolean,
    pendingOptions: BackupOptions?,
    onDismiss: () -> Unit,
    onGoToConfirm: (BackupOptions) -> Unit,
    onFinalConfirm: () -> Unit
) {
    val fixedCardBg = Color(0xFF1E1E1E)
    val fixedTitleColor = Color(0xFFF5F5F5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // STEP 1: Selection (Fade exit only)
            var activities by remember { mutableStateOf(true) }
            var tags by remember { mutableStateOf(true) }
            var goals by remember { mutableStateOf(true) }
            var logs by remember { mutableStateOf(true) }
            var settings by remember { mutableStateOf(true) }

            AnimatedVisibility(
                visible = !isConfirmStep,
                enter = fadeIn(tween(0)),
                exit = fadeOut(tween(1500))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = fixedCardBg
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.import_title), color = fixedTitleColor, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.import_confirm_desc), color = fixedTitleColor.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = activities, onCheckedChange = { activities = it })
                            Text(stringResource(R.string.export_activities), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tags, onCheckedChange = { tags = it })
                            Text(stringResource(R.string.export_tags), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = goals, onCheckedChange = { goals = it })
                            Text(stringResource(R.string.export_goals), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = logs, onCheckedChange = { logs = it })
                            Text(stringResource(R.string.export_logs), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = settings, onCheckedChange = { settings = it })
                            Text(stringResource(R.string.export_settings), color = fixedTitleColor)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                            TextButton(onClick = { onGoToConfirm(BackupOptions(activities, tags, goals, logs, settings)) }) {
                                Text(stringResource(R.string.import_button))
                            }
                        }
                    }
                }
            }

            // STEP 2: Confirmation (Instant appearance)
            if (isConfirmStep && pendingOptions != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = fixedCardBg
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.import_confirm_title), color = fixedTitleColor, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.import_confirm_desc), color = fixedTitleColor.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.import_confirm_question), color = fixedTitleColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                            TextButton(onClick = onFinalConfirm) { Text(stringResource(R.string.continue_button)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClearDataFlowContainer(
    isConfirmStep: Boolean,
    pendingOptions: BackupOptions?,
    onDismiss: () -> Unit,
    onGoToConfirm: (BackupOptions) -> Unit,
    onFinalConfirm: () -> Unit
) {
    val fixedCardBg = Color(0xFF1E1E1E)
    val fixedTitleColor = Color(0xFFF5F5F5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // STEP 1: Selection
            var activities by remember { mutableStateOf(false) }
            var tags by remember { mutableStateOf(false) }
            var goals by remember { mutableStateOf(false) }
            var logs by remember { mutableStateOf(false) }
            var settings by remember { mutableStateOf(false) }

            AnimatedVisibility(
                visible = !isConfirmStep,
                enter = fadeIn(tween(0)),
                exit = fadeOut(tween(1500))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = fixedCardBg
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.clear_data_title), color = fixedTitleColor, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = activities, onCheckedChange = { activities = it })
                            Text(stringResource(R.string.export_activities), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tags, onCheckedChange = { tags = it })
                            Text(stringResource(R.string.export_tags), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = goals, onCheckedChange = { goals = it })
                            Text(stringResource(R.string.export_goals), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = logs, onCheckedChange = { logs = it })
                            Text(stringResource(R.string.export_logs), color = fixedTitleColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = settings, onCheckedChange = { settings = it })
                            Text(stringResource(R.string.export_settings), color = fixedTitleColor)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                            TextButton(
                                onClick = { onGoToConfirm(BackupOptions(activities, tags, goals, logs, settings)) },
                                enabled = activities || tags || goals || logs || settings
                            ) {
                                Text(stringResource(R.string.clear_button), color = Color.Red)
                            }
                        }
                    }
                }
            }

            // STEP 2: Confirmation
            if (isConfirmStep && pendingOptions != null) {
                val deletedItems = mutableListOf<String>()
                if (pendingOptions.activities) deletedItems.add(stringResource(R.string.export_activities))
                if (pendingOptions.tags) deletedItems.add(stringResource(R.string.export_tags))
                if (pendingOptions.goals) deletedItems.add(stringResource(R.string.export_goals))
                if (pendingOptions.logs) deletedItems.add(stringResource(R.string.export_logs))
                if (pendingOptions.settings) deletedItems.add(stringResource(R.string.export_settings))

                Surface(
                    modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = fixedCardBg
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.clear_confirm_title), color = fixedTitleColor, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.clear_confirm_desc, deletedItems.joinToString(", ")), color = fixedTitleColor.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) }
                            TextButton(onClick = onFinalConfirm) { Text(stringResource(R.string.clear_button), color = Color.Red) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportDialog(
    onDismiss: () -> Unit,
    onExport: (
        activities: Boolean,
        tags: Boolean,
        goals: Boolean,
        logs: Boolean,
        settings: Boolean
    ) -> Unit,
    onImport: () -> Unit,
) {
    var activities by remember { mutableStateOf(true) }
    var tags by remember { mutableStateOf(true) }
    var goals by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(true) }

    val fixedCardBg = Color(0xFF1E1E1E)
    val fixedTitleColor = Color(0xFFF5F5F5)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = fixedCardBg,
        title = { Text(stringResource(R.string.settings_import_export), color = fixedTitleColor) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = activities, onCheckedChange = { activities = it })
                    Text(stringResource(R.string.export_activities), color = fixedTitleColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tags, onCheckedChange = { tags = it })
                    Text(stringResource(R.string.export_tags), color = fixedTitleColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = goals, onCheckedChange = { goals = it })
                    Text(stringResource(R.string.export_goals), color = fixedTitleColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = logs, onCheckedChange = { logs = it })
                    Text(stringResource(R.string.export_logs), color = fixedTitleColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = settings, onCheckedChange = { settings = it })
                    Text(stringResource(R.string.export_settings), color = fixedTitleColor)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onExport(
                    activities,
                    tags,
                    goals,
                    logs,
                    settings
                )
                onDismiss()
            }) {
                Text(stringResource(R.string.export_button))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onImport) {
                    Text(stringResource(R.string.import_button))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        }
    )
}

@Composable
private fun SettingsColorCard(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    val fixedCardBg = Color(0xFF1E1E1E)
    val fixedTitleColor = Color(0xFFF5F5F5)
    val fixedSubtitleColor = Color(0xFFB0B0B0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = fixedCardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = fixedTitleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = fixedSubtitleColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    val fixedCardBg = Color(0xFF1E1E1E)
    val fixedTitleColor = Color(0xFFF5F5F5)
    val fixedSubtitleColor = Color(0xFFB0B0B0)

    val animatedBg by animateColorAsState(
        targetValue = if (highlight) Color(0xFFA4A4A4) else fixedCardBg,
        animationSpec = tween(durationMillis = 300),
        label = "bgColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = animatedBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = fixedTitleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = fixedSubtitleColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fixedSubtitleColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ActitrackerTheme {
        // Preview with dummy data
    }
}
