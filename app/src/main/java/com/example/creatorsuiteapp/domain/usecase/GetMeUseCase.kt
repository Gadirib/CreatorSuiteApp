package com.example.creatorsuiteapp.domain.usecase

import com.example.creatorsuiteapp.domain.repository.ContentRepository

class GetMeUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke() = repo.getMe()
}
