package com.example.tiktokclone.domain.model

data class SavedVideoItem(
    val videoRes: Int,     // mp4 in res/raw
    val thumbRes: Int      // thumbnail in res/drawable
)

data class SavedImageItem(
    val imageRes: Int      // image in res/drawable
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
