package com.example.tiktokclone.domain.model

data class ContentItem(
    val id: String,
    val thumbnailUrl: String? = null,
    val likes: Int = 0,
    val createdAt: Long = 0L
)
