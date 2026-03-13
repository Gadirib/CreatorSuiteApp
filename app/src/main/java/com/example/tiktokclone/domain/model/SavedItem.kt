package com.example.tiktokclone.domain.model

data class SavedItem(
    val id: String,
    val creatorId: String,
    val type: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val createdAt: String
)
