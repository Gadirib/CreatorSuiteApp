package com.example.creatorsuiteapp.domain.usecase

import com.example.creatorsuiteapp.domain.repository.ContentRepository

class GetCleanJobUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke(id: String) = repo.getCleanJob(id)
}
