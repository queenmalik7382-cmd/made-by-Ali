package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" | "assistant" | "system" | "tool"
    val content: String,
    val source: String = "chat", // "chat" | "voice" | "scan"
    val attachmentUri: String? = null,
    val attachmentType: String? = null, // "image" | "pdf" | "markdown"
    val attachmentName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "captured_notifications")
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val sender: String,
    val text: String,
    val wasReadAloud: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_tasks")
data class RoutineTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val daysCsv: String = "1,2,3,4,5,6,7", // ISO day 1..7
    val streakCount: Int = 0,
    val lastCompletedEpochDay: Long = -1L,
    val isSkippedToday: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "learned_tasks")
data class LearnedTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryTag: String, // e.g., "sawaal", "video prompt", "image prompt", "custom"
    val stepsJson: String,   // JSON array of action steps
    val stepCount: Int,
    val isBuiltIn: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factKey: String,
    val factValue: String,
    val category: String = "preference", // "preference" | "person" | "habit" | "note"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "assistant_rules")
data class AssistantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleText: String,
    val isStrictBlock: Boolean = true,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_notes")
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val transcript: String,
    val summary: String,
    val wasReceptionistHandled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_announcements")
data class AppAnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val relativeAgeLabel: String = "Abhi",
    val isUnread: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
