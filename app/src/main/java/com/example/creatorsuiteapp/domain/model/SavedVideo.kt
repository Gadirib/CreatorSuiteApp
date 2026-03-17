package com.example.creatorsuiteapp.domain.model

import java.util.Date


data class SavedVideo(
    val id: String,
    val fileName: String,
    val thumbnailFileName: String,
    val duration: Double,
    val createdAt: Date,
    val postedToTikTok: Boolean = false,
    val tiktokItemId: String? = null,
    val tiktokIntegrationKind: String? = null,
    val tiktokUsername: String? = null,
    val tiktokPublishedAt: Date? = null
)