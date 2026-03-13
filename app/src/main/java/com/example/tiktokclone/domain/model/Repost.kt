package com.example.tiktokclone.domain.model

data class Repost(
    val id: String,
    val tiktokPostId: String,
    val caption: String,
    val authorUsername: String,
    val likes: Long,
    val durationSec: Double,
    val createdAt: String,
    val thumbnailUrl: String,
    val tiktokUrl: String
)

