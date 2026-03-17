package com.example.creatorsuiteapp.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


object RepostStatsRepository {

    private const val TAG = "RepostStats"
    private const val PREF_NAME = "repost_stats"

    data class AccountStats(
        val accountId: String,
        val deletedCount: Int
    )

    private val _stats = MutableStateFlow<AccountStats?>(null)
    val stats: StateFlow<AccountStats?> = _stats

    private fun accountId(context: Context): String {
        val prefs = context.getSharedPreferences("tiktok_session", Context.MODE_PRIVATE)
        val secUid = prefs.getString("secUid", null)
        return if (!secUid.isNullOrBlank()) secUid.takeLast(16) else "default"
    }

    fun load(context: Context) {
        val id = accountId(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("deleted_$id", 0)
        _stats.value = AccountStats(accountId = id, deletedCount = count)
        Log.d(TAG, "Loaded stats for $id: deletedCount=$count")
    }

    fun incrementDeleted(context: Context, count: Int = 1) {
        val id = accountId(context)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt("deleted_$id", 0)
        val newCount = current + count
        prefs.edit().putInt("deleted_$id", newCount).apply()
        _stats.value = AccountStats(accountId = id, deletedCount = newCount)
        Log.d(TAG, "Incremented deleted for $id: $current → $newCount")
    }

    fun getDeletedCount(context: Context, secUid: String): Int {
        val id = secUid.takeLast(16)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("deleted_$id", 0)
    }

    fun reset(context: Context) {
        val id = accountId(context)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("deleted_$id", 0).apply()
        _stats.value = AccountStats(accountId = id, deletedCount = 0)
    }

    fun switchAccount(context: Context) = load(context)
}