package com.example.creatorsuiteapp.domain.model

data class Post(
    val id: String,
    val tiktokPostId: String,
    val creatorId: String,
    val caption: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val durationSec: Double,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val createdAt: String,
    val status: String
)
