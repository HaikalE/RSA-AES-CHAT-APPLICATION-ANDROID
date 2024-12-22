package com.example.imrsaaes.util

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.*

class CustomDateAdapter : TypeAdapter<Date>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun write(out: JsonWriter, value: Date?) {
        out.value(value?.let { dateFormat.format(it) })
    }

    override fun read(input: JsonReader): Date? {
        val dateStr = input.nextString()
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null // Handle error jika parsing gagal
        }
    }
}
