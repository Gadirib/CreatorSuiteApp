package com.example.creatorsuiteapp.domain.model

data class UploadSession(
    val uploadId: String,
    val uploadUrl: String,
    val chunkSizeBytes: Int
)

