package com.example.creatorsuiteapp.domain.model

data class ImportFile(
    val id: String,
    val provider: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAt: String,
    val downloadUrl: String? = null
)

