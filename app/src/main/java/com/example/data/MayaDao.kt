package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MayaDao {
    // Chat & Shared Voice History
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    // Captured System Notifications (WhatsApp / SMS / Gmail)
    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC LIMIT 100")
    fun observeCapturedNotifications(): Flow<List<CapturedNotificationEntity>>

    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(limit: Int = 10): List<CapturedNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedNotification(item: CapturedNotificationEntity): Long

    @Query("DELETE FROM captured_notifications")
    suspend fun clearCapturedNotifications()

    // Daily Routine ("Meri Routine")
    @Query("SELECT * FROM routine_tasks ORDER BY hour ASC, minute ASC")
    fun observeRoutineTasks(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY hour ASC, minute ASC")
    suspend fun getAllRoutineTasks(): List<RoutineTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTask(task: RoutineTaskEntity): Long

    @Update
    suspend fun updateRoutineTask(task: RoutineTaskEntity)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun deleteRoutineTask(id: Long)

    // Yaadein (Learned Accessibility Tasks)
    @Query("SELECT * FROM learned_tasks ORDER BY isBuiltIn DESC, timestamp DESC")
    fun observeLearnedTasks(): Flow<List<LearnedTaskEntity>>

    @Query("SELECT * FROM learned_tasks")
    suspend fun getAllLearnedTasks(): List<LearnedTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearnedTask(task: LearnedTaskEntity): Long

    @Update
    suspend fun updateLearnedTask(task: LearnedTaskEntity)

    @Query("DELETE FROM learned_tasks WHERE id = :id")
    suspend fun deleteLearnedTask(id: Long)

    // User Long-term Memory
    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun observeMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<UserMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    // Maya ke Rules
    @Query("SELECT * FROM assistant_rules ORDER BY timestamp DESC")
    fun observeRules(): Flow<List<AssistantRuleEntity>>

    @Query("SELECT * FROM assistant_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<AssistantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AssistantRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AssistantRuleEntity)

    @Query("DELETE FROM assistant_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    // Receptionist Call Notes
    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    fun observeCallNotes(): Flow<List<CallNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallNote(note: CallNoteEntity): Long

    // In-App Announcements / Feed
    @Query("SELECT * FROM app_announcements ORDER BY timestamp DESC")
    fun observeAnnouncements(): Flow<List<AppAnnouncementEntity>>

    @Query("SELECT COUNT(*) FROM app_announcements")
    suspend fun countAnnouncements(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(item: AppAnnouncementEntity): Long

    @Query("DELETE FROM app_announcements")
    suspend fun clearAnnouncements()
}
