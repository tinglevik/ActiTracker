package com.example.actitracker.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

class DataBackupManager(
    private val dao: ActivityDao,
    private val settingsDataStore: SettingsDataStore
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class BackupData(
        val version: Int = 1,
        val activities: List<ActivityEntity>? = null,
        val tags: List<TagEntity>? = null,
        val goals: List<GoalEntity>? = null,
        val activityLogs: List<ActivityLogEntity>? = null,
        val activityTagCrossRefs: List<ActivityTagCrossRef>? = null,
        val settings: Map<String, Any>? = null
    )

    suspend fun createBackup(
        exportActivities: Boolean,
        exportTags: Boolean,
        exportGoals: Boolean,
        exportLogs: Boolean,
        exportSettings: Boolean
    ): String {
        val backupData = BackupData(
            activities = if (exportActivities) dao.getAllActivitiesSync() else null,
            tags = if (exportTags) dao.getAllTagsSync() else null,
            goals = if (exportGoals) dao.getAllGoalsSync() else null,
            activityLogs = if (exportLogs) dao.getAllSessions() else null,
            activityTagCrossRefs = if (exportTags || exportActivities) dao.getAllActivityTagCrossRefsSync() else null,
            settings = if (exportSettings) settingsDataStore.getAllSettings() else null
        )
        return gson.toJson(backupData)
    }

    suspend fun restoreBackup(
        json: String,
        importActivities: Boolean,
        importTags: Boolean,
        importGoals: Boolean,
        importLogs: Boolean,
        importSettings: Boolean
    ) {
        val backupData = gson.fromJson(json, BackupData::class.java)
        
        if (importTags) backupData.tags?.let { dao.insertTags(it) }
        if (importActivities) backupData.activities?.let { dao.insertActivities(it) }
        if (importTags || importActivities) backupData.activityTagCrossRefs?.let { dao.insertActivityTagCrossRefs(it) }
        if (importGoals) backupData.goals?.let { dao.insertGoals(it) }
        if (importLogs) backupData.activityLogs?.let { dao.insertSessions(it) }
        if (importSettings) backupData.settings?.let { settingsDataStore.restoreSettings(it) }
    }

    suspend fun clearData(
        activities: Boolean,
        tags: Boolean,
        goals: Boolean,
        logs: Boolean,
        settings: Boolean
    ) {
        if (activities) dao.clearAllActivities()
        if (tags) dao.clearAllTags()
        if (activities || tags) dao.clearAllActivityTagRefs()
        if (goals) dao.clearAllGoals()
        if (logs) dao.clearAllLogs()
        if (settings) settingsDataStore.clearAllSettings()
    }
}
