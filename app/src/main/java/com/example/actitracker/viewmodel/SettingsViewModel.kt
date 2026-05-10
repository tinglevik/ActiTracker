package com.example.actitracker.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.actitracker.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.actitracker.data.DataBackupManager
import com.example.actitracker.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: DataBackupManager
) : ViewModel() {

    private val _backgroundColor = MutableStateFlow(Color(SettingsDataStore.DEFAULT_COLOR_ARGB))
    val backgroundColor: StateFlow<Color> = _backgroundColor

    private val _contentColor = MutableStateFlow(Color.Black)
    val contentColor: StateFlow<Color> = _contentColor

    private val _showWarningDrawer = MutableStateFlow(false)
    val showWarningDrawer: StateFlow<Boolean> = _showWarningDrawer

    private val _previousBg = MutableStateFlow(Color(SettingsDataStore.DEFAULT_COLOR_ARGB))

    private val _previousText = MutableStateFlow(Color.Black)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _dimScreen = MutableStateFlow(false)
    val dimScreen: StateFlow<Boolean> = _dimScreen

    private val _highlightDataManagement = MutableStateFlow(false)
    val highlightDataManagement: StateFlow<Boolean> = _highlightDataManagement

    init {
        viewModelScope.launch {
            settingsDataStore.backgroundColorFlow.collect { argb ->
                _backgroundColor.value = Color(argb)
            }
        }
        viewModelScope.launch {
            settingsDataStore.contentColorFlow.collect { argb ->
                _contentColor.value = Color(argb)
            }
        }
    }

    fun saveBackgroundColor(color: Color) {
        viewModelScope.launch {
            _backgroundColor.value = color
            settingsDataStore.saveBackgroundColor(color.toArgbInt())
        }
    }

    fun saveContentColor(color: Color) {
        viewModelScope.launch {
            _contentColor.value = color
            settingsDataStore.saveContentColor(color.toArgbInt())
        }
    }

    fun showWarning(previousBg: Color, previousText: Color) {
        _previousBg.value = previousBg
        _previousText.value = previousText
        _showWarningDrawer.value = true
    }

    fun hideWarning() {
        _showWarningDrawer.value = false
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun revertChanges(context: Context) {
        saveBackgroundColor(_previousBg.value)
        saveContentColor(_previousText.value)
        hideWarning()
        showSnackbar(context.getString(R.string.settings_reverted))
    }

    fun keepChanges(context: Context) {
        hideWarning()
        showSnackbar(context.getString(R.string.settings_saved))
    }

    fun triggerHighlight() {
        viewModelScope.launch {
            repeat(2) {
                _highlightDataManagement.value = true
                kotlinx.coroutines.delay(500)
                _highlightDataManagement.value = false
                kotlinx.coroutines.delay(400)
            }
        }
    }

    fun createBackup(
        activities: Boolean,
        tags: Boolean,
        goals: Boolean,
        logs: Boolean,
        settings: Boolean,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val json = backupManager.createBackup(activities, tags, goals, logs, settings)
                onResult(json)
            } catch (e: Exception) {
                showSnackbar("Export failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(
        json: String,
        activities: Boolean,
        tags: Boolean,
        goals: Boolean,
        logs: Boolean,
        settings: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                backupManager.restoreBackup(json, activities, tags, goals, logs, settings)
                _dimScreen.value = true
                showSnackbar(context.getString(R.string.import_success))
                kotlinx.coroutines.delay(1500)
                _dimScreen.value = false
            } catch (e: Exception) {
                showSnackbar("Import failed: ${e.message}")
            }
        }
    }

    private fun Color.toArgbInt(): Int {
        return android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
}
