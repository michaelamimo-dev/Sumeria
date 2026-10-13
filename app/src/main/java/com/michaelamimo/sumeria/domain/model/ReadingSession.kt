package com.michaelamimo.sumeria.domain.model

enum class SessionMode { TEXT, AUDIO }

/** One completed or in-progress reading interval. Duration is recorded in milliseconds. */
data class ReadingSession(
    val id: String,
    val bookId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val durationMillis: Long = 0,
    val pagesRead: Int = 0,
    val wordsRead: Int = 0,
    val mode: SessionMode = SessionMode.TEXT
)
