package com.example.creatorsuiteapp.data

import com.example.creatorsuiteapp.data.repository.VisualContentRepository
import com.example.creatorsuiteapp.domain.repository.ContentRepository

object ServiceLocator {
    val contentRepository: ContentRepository by lazy {
        VisualContentRepository()
    }
}
