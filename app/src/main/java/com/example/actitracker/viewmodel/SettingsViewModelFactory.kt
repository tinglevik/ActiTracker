package com.example.actitracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.actitracker.data.DataBackupManager
import com.example.actitracker.data.SettingsDataStore

class SettingsViewModelFactory(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: DataBackupManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsDataStore, backupManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
