package com.example.creatorsuiteapp.domain.model

data class CleanJob(
    val id: String,
    val creatorId: String,
    val postIds: List<String>,
    val status: String,
    val progress: Double,
    val createdAt: String
)
