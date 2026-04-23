// com/example/prob1/data/database/Converters.kt
package com.example.prob1.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {

    private val gson = Gson()

    // ==================== Date ====================

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // ==================== List<String> ====================

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        if (json.isEmpty() || json == "[]") {
            return emptyList()
        }
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== Map<String, Any> ====================

    @TypeConverter
    fun fromMap(map: Map<String, Any>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMap(json: String): Map<String, Any> {
        if (json.isEmpty() || json == "{}") {
            return emptyMap()
        }
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ==================== List<Map<String, Any>> ====================

    @TypeConverter
    fun fromMapList(list: List<Map<String, Any>>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toMapList(json: String): List<Map<String, Any>> {
        if (json.isEmpty() || json == "[]") {
            return emptyList()
        }
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}