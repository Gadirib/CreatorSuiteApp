package com.example.creatorsuiteapp.domain.usecase

import com.example.creatorsuiteapp.domain.repository.ContentRepository

class CreateCleanJobUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke(postIds: List<String>) = repo.createCleanJob(postIds)
}
