package com.example.tiktokclone.domain.usecase

import com.example.tiktokclone.domain.repository.ContentRepository

class GetCleanJobUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke(id: String) = repo.getCleanJob(id)
}
