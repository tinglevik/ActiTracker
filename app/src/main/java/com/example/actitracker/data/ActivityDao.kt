package com.example.actitracker.data

import androidx.room.*
import com.example.actitracker.data.dto.ActivityDuration
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities ORDER BY sortOrder ASC, id ASC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities")
    suspend fun getAllActivitiesSync(): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityAndGetId(activity: ActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Update
    suspend fun updateActivities(activities: List<ActivityEntity>)

    @Transaction
    suspend fun deleteActivityWithSessions(id: Long) {
        deleteSessionsByActivityId(id)
        deleteActivity(id)
        deleteActivityTagRefs(id)
    }

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: Long)

    @Query("DELETE FROM activity_log WHERE activityId = :activityId")
    suspend fun deleteSessionsByActivityId(activityId: Long)

    @Query("DELETE FROM activity_tag_cross_ref WHERE activityId = :activityId")
    suspend fun deleteActivityTagRefs(activityId: Long)

    // Tags
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, id ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM activity_tag_cross_ref")
    suspend fun getAllActivityTagCrossRefsSync(): List<ActivityTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Update
    suspend fun updateTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityTagCrossRef(crossRef: ActivityTagCrossRef)

    @Query("DELETE FROM activity_tag_cross_ref WHERE activityId = :activityId AND tagId = :tagId")
    suspend fun deleteActivityTagCrossRef(activityId: Long, tagId: Long)

    @Query("""
        SELECT tags.* FROM tags 
        INNER JOIN activity_tag_cross_ref ON tags.id = activity_tag_cross_ref.tagId 
        WHERE activity_tag_cross_ref.activityId = :activityId
    """)
    fun getTagsForActivity(activityId: Long): Flow<List<TagEntity>>

    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsSync(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityTagCrossRefs(crossRefs: List<ActivityTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ActivityLogEntity>)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)

    @Query("SELECT * FROM activity_log WHERE activityId = :activityId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(activityId: Long): ActivityLogEntity?

    @Query("SELECT * FROM activity_log WHERE activityId = :activityId")
    suspend fun getSessions(activityId: Long): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_log")
    suspend fun getAllSessions(): List<ActivityLogEntity>

    @Insert
    suspend fun insertSession(session: ActivityLogEntity)

    @Update
    suspend fun updateSession(session: ActivityLogEntity)

    @Query("SELECT * FROM activity_log WHERE (startTime <= :to AND (endTime IS NULL OR endTime >= :from))")
    suspend fun getAllSessionsInInterval(from: Long, to: Long): List<ActivityLogEntity>

    @Query("""
        SELECT activityId, DATE(startTime / 1000, 'unixepoch') as day,
        SUM(COALESCE(endTime, :now) - startTime) as duration
        FROM activity_log
        WHERE startTime >= :from AND startTime <= :to
        GROUP BY activityId, day
    """)
    suspend fun getActivityDurations(
        from: Long,
        to: Long,
        now: Long = System.currentTimeMillis()
    ): List<ActivityDuration>

    @Query("""
        SELECT * FROM activity_log
        WHERE activityId = :activityId
        AND startTime >= :from
        AND startTime <= :to
        ORDER BY startTime DESC
    """)
    suspend fun getSessionsForPeriod(
        activityId: Long,
        from: Long,
        to: Long
    ): List<ActivityLogEntity>

    @Query("""
        SELECT * FROM activity_log
        WHERE (endTime IS NULL OR endTime >= :from)
        ORDER BY startTime DESC
    """)
    fun getSessionsFromFlow(from: Long): Flow<List<ActivityLogEntity>>

    @Query("""
        SELECT * FROM activity_log 
        WHERE endTime IS NULL 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getAnyActiveSession(): ActivityLogEntity?

    @Query("""
        SELECT * FROM activity_log 
        WHERE endTime IS NULL 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    fun getActiveSessionFlow(): Flow<ActivityLogEntity?>

    @Query("SELECT * FROM activity_log WHERE endTime IS NULL")
    suspend fun getAllActiveSessions(): List<ActivityLogEntity>

    @Query("UPDATE activity_log SET endTime = :endTime WHERE endTime IS NULL AND activityId != :excludeActivityId")
    suspend fun closeAllActiveSessionsExcept(excludeActivityId: Long, endTime: Long)

    @Query("UPDATE activity_log SET endTime = :endTime WHERE endTime IS NULL")
    suspend fun closeAllActiveSessions(endTime: Long)

    @Query("UPDATE activity_log SET endTime = :endTime WHERE id = :sessionId")
    suspend fun closeSessionById(sessionId: Long, endTime: Long)

    @Query("SELECT * FROM activities WHERE showInQuickPanel = 1")
    fun getQuickPanelActivities(): Flow<List<ActivityEntity>>

    @Query("UPDATE activities SET showInQuickPanel = :show WHERE id = :id")
    suspend fun setShowInQuickPanel(id: Long, show: Boolean)

    @Query("DELETE FROM activities")
    suspend fun clearAllActivities()

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM activity_tag_cross_ref")
    suspend fun clearAllActivityTagRefs()

    @Query("DELETE FROM goals")
    suspend fun clearAllGoals()

    @Query("DELETE FROM activity_log")
    suspend fun clearAllLogs()
}
