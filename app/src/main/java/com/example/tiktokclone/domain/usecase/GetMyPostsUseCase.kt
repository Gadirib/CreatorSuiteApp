package com.example.tiktokclone.domain.usecase

import com.example.tiktokclone.domain.repository.ContentRepository

class GetMyPostsUseCase(private val repo: ContentRepository) {
    suspend operator fun invoke() = repo.getMyPosts()
}
