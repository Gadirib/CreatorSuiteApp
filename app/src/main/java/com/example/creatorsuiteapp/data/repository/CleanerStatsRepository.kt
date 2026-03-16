package com.example.creatorsuiteapp.data.repository

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CleanerDayStat(
    val dayLabel: String,
    val dateKey: String,
    val count: Int
)

object CleanerStatsRepository {
    private const val PREF_NAME = "cleaner_prefs"
    private const val DAILY_PREFIX = "history_"
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun recordDeletion(context: Context, count: Int = 1) {
        if (count <= 0) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val todayKey = today()
        val current = prefs.getInt("$DAILY_PREFIX$todayKey", 0)
        prefs.edit().putInt("$DAILY_PREFIX$todayKey", current + count).apply()
    }

    fun getLast7DaysStats(context: Context): List<CleanerDayStat> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        return buildList {
            repeat(7) {
                val date = calendar.time
                val dateKey = dayFormat.format(date)
                add(
                    CleanerDayStat(
                        dayLabel = SimpleDateFormat("E", Locale.getDefault()).format(date).take(1),
                        dateKey = dateKey,
                        count = prefs.getInt("$DAILY_PREFIX$dateKey", 0)
                    )
                )
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    fun getTotalCleaned(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.all.entries
            .filter { it.key.startsWith(DAILY_PREFIX) }
            .sumOf { (it.value as? Int) ?: 0 }
    }

    private fun today(): String = dayFormat.format(Date())
}
