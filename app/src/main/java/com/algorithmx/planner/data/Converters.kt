package com.algorithmx.planner.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    // --- LocalDate (YYYY-MM-DD) ---
    @TypeConverter
    fun fromDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    // --- LocalDateTime (YYYY-MM-DDTHH:MM:SS) ---
    @TypeConverter
    fun fromDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }

    @TypeConverter
    fun dateTimeToString(dateTime: LocalDateTime?): String? = dateTime?.toString()
}