package com.example.actitracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val BACKGROUND_COLOR_KEY = intPreferencesKey("background_color")
        private val CONTENT_COLOR_KEY = intPreferencesKey("content_color")

        private val FIRST_START_TIMES_KEY = stringPreferencesKey("first_start_times")

        val DEFAULT_COLOR_ARGB: Int = 0xFFFFFBFE.toInt()
        val DEFAULT_CONTENT_COLOR_ARGB: Int = 0xFF1C1B1F.toInt()
    }

    val backgroundColorFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BACKGROUND_COLOR_KEY] ?: DEFAULT_COLOR_ARGB
    }

    val contentColorFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[CONTENT_COLOR_KEY] ?: DEFAULT_CONTENT_COLOR_ARGB
    }

    val firstStartTimesFlow: Flow<Map<Long, Long>> = context.dataStore.data.map { prefs ->
        val json = prefs[FIRST_START_TIMES_KEY] ?: return@map emptyMap()
        parseFirstStartTimes(json)
    }

    suspend fun saveBackgroundColor(colorArgb: Int) {
        context.dataStore.edit { prefs ->
            prefs[BACKGROUND_COLOR_KEY] = colorArgb
        }
    }

    suspend fun saveContentColor(colorArgb: Int) {
        context.dataStore.edit { prefs ->
            prefs[CONTENT_COLOR_KEY] = colorArgb
        }
    }

    suspend fun saveFirstStartTimes(times: Map<Long, Long>) {
        context.dataStore.edit { prefs ->
            prefs[FIRST_START_TIMES_KEY] = serializeFirstStartTimes(times)
        }
    }

    suspend fun getAllSettings(): Map<String, Any> {
        val prefs = context.dataStore.data.first()
        val result = mutableMapOf<String, Any>()
        prefs[BACKGROUND_COLOR_KEY]?.let { result["background_color"] = it }
        prefs[CONTENT_COLOR_KEY]?.let { result["content_color"] = it }
        prefs[FIRST_START_TIMES_KEY]?.let { result["first_start_times"] = it }
        return result
    }

    suspend fun restoreSettings(settings: Map<String, Any>) {
        context.dataStore.edit { prefs ->
            (settings["background_color"] as? Number)?.toInt()?.let {
                prefs[BACKGROUND_COLOR_KEY] = it
            }
            (settings["content_color"] as? Number)?.toInt()?.let {
                prefs[CONTENT_COLOR_KEY] = it
            }
            (settings["first_start_times"] as? String)?.let {
                prefs[FIRST_START_TIMES_KEY] = it
            }
        }
    }

    private fun serializeFirstStartTimes(times: Map<Long, Long>): String {
        val json = JSONObject()
        times.forEach { (id, time) -> json.put(id.toString(), time) }
        return json.toString()
    }

    private fun parseFirstStartTimes(json: String): Map<Long, Long> {
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<Long, Long>()
            obj.keys().forEach { key ->
                result[key.toLong()] = obj.getLong(key)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
