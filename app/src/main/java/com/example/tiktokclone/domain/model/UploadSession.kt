package com.example.tiktokclone.domain.model

data class UploadSession(
    val uploadId: String,
    val uploadUrl: String,
    val chunkSizeBytes: Int
)

