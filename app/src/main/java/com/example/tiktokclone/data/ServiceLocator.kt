package com.example.tiktokclone.data

import com.example.tiktokclone.data.repository.VisualContentRepository
import com.example.tiktokclone.domain.repository.ContentRepository

object ServiceLocator {
    val contentRepository: ContentRepository by lazy {
        VisualContentRepository()
    }
}
