package com.example.tiktokclone.domain.usecase

import com.example.tiktokclone.domain.repository.ContentRepository

class CreateCleanJobUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke(postIds: List<String>) = repo.createCleanJob(postIds)
}
