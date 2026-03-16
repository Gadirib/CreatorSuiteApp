package com.example.creatorsuiteapp.domain.model

import java.util.Date

/**
 * Matches document section 8 — Структура SavedVideo (JSON)
 */
data class SavedVideo(
    val id: String,                          // UUID
    val fileName: String,                    // "{id}.mp4"
    val thumbnailFileName: String,           // "{id}.jpg"
    val duration: Double,                    // seconds
    val createdAt: Date,
    val postedToTikTok: Boolean = false,
    val tiktokItemId: String? = null,
    val tiktokIntegrationKind: String? = null, // "webSessionPrivateAPI" | "contentPostingAPI" | "shareKit"
    val tiktokUsername: String? = null,
    val tiktokPublishedAt: Date? = null
)