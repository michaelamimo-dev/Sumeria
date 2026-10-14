package com.michaelamimo.sumeria.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books RENAME TO books_v1")
        db.execSQL("ALTER TABLE reading_progress RENAME TO reading_progress_v1")
        db.execSQL("ALTER TABLE reading_sessions RENAME TO reading_sessions_v1")
        db.execSQL("ALTER TABLE reader_bookmarks RENAME TO reader_bookmarks_v1")

        db.execSQL("""CREATE TABLE books (
            ownerUserId TEXT NOT NULL DEFAULT 'local', id TEXT NOT NULL, title TEXT NOT NULL,
            authorsJson TEXT NOT NULL, description TEXT, format TEXT NOT NULL, sourceUri TEXT,
            coverUri TEXT, genre TEXT, totalPages INTEGER, addedAtMillis INTEGER NOT NULL,
            lastOpenedAtMillis INTEGER, PRIMARY KEY(ownerUserId, id))""")
        db.execSQL("""CREATE TABLE reading_progress (
            ownerUserId TEXT NOT NULL, bookId TEXT NOT NULL, locationJson TEXT,
            currentPage INTEGER, totalPages INTEGER, progression REAL NOT NULL,
            updatedAtMillis INTEGER NOT NULL, PRIMARY KEY(ownerUserId, bookId),
            FOREIGN KEY(ownerUserId, bookId) REFERENCES books(ownerUserId, id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE reading_sessions (
            ownerUserId TEXT NOT NULL, id TEXT NOT NULL, bookId TEXT NOT NULL,
            startedAtMillis INTEGER NOT NULL, endedAtMillis INTEGER, durationMillis INTEGER NOT NULL,
            pagesRead INTEGER NOT NULL, wordsRead INTEGER NOT NULL, mode TEXT NOT NULL,
            PRIMARY KEY(ownerUserId, id),
            FOREIGN KEY(ownerUserId, bookId) REFERENCES books(ownerUserId, id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
        db.execSQL("""CREATE TABLE reader_bookmarks (
            ownerUserId TEXT NOT NULL, id TEXT NOT NULL, bookId TEXT NOT NULL, locationJson TEXT NOT NULL,
            chapterTitle TEXT, excerpt TEXT, createdAtMillis INTEGER NOT NULL,
            PRIMARY KEY(ownerUserId, id),
            FOREIGN KEY(ownerUserId, bookId) REFERENCES books(ownerUserId, id) ON UPDATE NO ACTION ON DELETE CASCADE)""")

        db.execSQL("""INSERT INTO books (ownerUserId, id, title, authorsJson, description, format, sourceUri,
            coverUri, genre, totalPages, addedAtMillis, lastOpenedAtMillis)
            SELECT 'local', id, title, authorsJson, description, format, sourceUri, coverUri, genre,
            totalPages, addedAtMillis, lastOpenedAtMillis FROM books_v1""")
        db.execSQL("""INSERT INTO reading_progress (ownerUserId, bookId, locationJson, currentPage, totalPages, progression, updatedAtMillis)
            SELECT 'local', bookId, locationJson, currentPage, totalPages, progression, updatedAtMillis FROM reading_progress_v1""")
        db.execSQL("""INSERT INTO reading_sessions (ownerUserId, id, bookId, startedAtMillis, endedAtMillis,
            durationMillis, pagesRead, wordsRead, mode)
            SELECT 'local', id, bookId, startedAtMillis, endedAtMillis, durationMillis, pagesRead, wordsRead, mode FROM reading_sessions_v1""")
        db.execSQL("""INSERT INTO reader_bookmarks (ownerUserId, id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis)
            SELECT 'local', id, bookId, locationJson, chapterTitle, excerpt, createdAtMillis FROM reader_bookmarks_v1""")

        db.execSQL("DROP TABLE reading_progress_v1")
        db.execSQL("DROP TABLE reading_sessions_v1")
        db.execSQL("DROP TABLE reader_bookmarks_v1")
        db.execSQL("DROP TABLE books_v1")
        db.execSQL("CREATE INDEX index_reading_progress_ownerUserId_bookId ON reading_progress(ownerUserId, bookId)")
        db.execSQL("CREATE INDEX index_reading_sessions_ownerUserId_bookId ON reading_sessions(ownerUserId, bookId)")
        db.execSQL("CREATE INDEX index_reading_sessions_startedAtMillis ON reading_sessions(startedAtMillis)")
        db.execSQL("CREATE INDEX index_reader_bookmarks_ownerUserId_bookId ON reader_bookmarks(ownerUserId, bookId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_preferences ADD COLUMN autoCloudSyncEnabled INTEGER NOT NULL DEFAULT 1")
    }
}
