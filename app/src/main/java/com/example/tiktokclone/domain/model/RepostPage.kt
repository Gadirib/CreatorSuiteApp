package com.example.tiktokclone.domain.model

data class RepostPage(
    val items: List<Repost>,
    val cursor: Int,
    val hasMore: Boolean
)

