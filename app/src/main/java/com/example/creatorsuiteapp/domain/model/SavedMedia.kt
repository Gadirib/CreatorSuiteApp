package com.example.creatorsuiteapp.domain.model

data class SavedVideoItem(
    val videoRes: Int,
    val thumbRes: Int
)

data class SavedImageItem(
    val imageRes: Int
)

data class CleanerItem(
    val postId: String,
    val name: String,
    val days: String,
    val likes: String,
    val thumbRes: Int,
    val duration: String,
    val tiktokUrl: String? = null
)
