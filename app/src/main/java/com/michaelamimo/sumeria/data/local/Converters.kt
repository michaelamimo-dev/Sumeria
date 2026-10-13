package com.michaelamimo.sumeria.data.local

import androidx.room.TypeConverter
import com.michaelamimo.sumeria.domain.model.BookFormat
import com.michaelamimo.sumeria.domain.model.SessionMode

class Converters {
    @TypeConverter fun bookFormatToString(value: BookFormat): String = value.name
    @TypeConverter fun stringToBookFormat(value: String): BookFormat = BookFormat.valueOf(value)
    @TypeConverter fun sessionModeToString(value: SessionMode): String = value.name
    @TypeConverter fun stringToSessionMode(value: String): SessionMode = SessionMode.valueOf(value)
}
