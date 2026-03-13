package com.example.tiktokclone.data.repository

import com.example.tiktokclone.domain.model.CleanJob
import com.example.tiktokclone.domain.model.Creator
import com.example.tiktokclone.domain.model.ImportFile
import com.example.tiktokclone.domain.model.MediaUpload
import com.example.tiktokclone.domain.model.Post
import com.example.tiktokclone.domain.model.PublishResult
import com.example.tiktokclone.domain.model.Repost
import com.example.tiktokclone.domain.model.RepostPage
import com.example.tiktokclone.domain.model.SavedItem
import com.example.tiktokclone.domain.model.UploadSession
import com.example.tiktokclone.domain.repository.ContentRepository
import java.time.Instant
import java.util.UUID
import kotlin.math.absoluteValue

class VisualContentRepository : ContentRepository {
    private val creator = Creator(
        id = "creator-1",
        name = "@name",
        avatarUrl = null
    )

    private val posts = mutableListOf(
        samplePost(id = "post-1", caption = "Caption", likes = 120, createdAt = "2026-02-27T12:00:00Z"),
        samplePost(id = "post-2", caption = "Caption 2", likes = 22, createdAt = "2026-02-26T12:00:00Z"),
        samplePost(id = "post-3", caption = "Caption 3", likes = 310, createdAt = "2026-02-25T12:00:00Z"),
        samplePost(id = "post-4", caption = "Caption 4", likes = 44, createdAt = "2026-02-24T12:00:00Z"),
        samplePost(id = "post-5", caption = "Caption 5", likes = 85, createdAt = "2026-02-23T12:00:00Z"),
        samplePost(id = "post-6", caption = "Caption 6", likes = 12, createdAt = "2026-02-22T12:00:00Z")
    )

    private val reposts = mutableListOf(
        Repost(
            id = "repost-1",
            tiktokPostId = "tt-repost-1",
            caption = "@Name",
            authorUsername = "@creator_a",
            likes = 1200,
            durationSec = 12.0,
            createdAt = "2026-02-27T12:00:00Z",
            thumbnailUrl = "local://thumb/repost-1",
            tiktokUrl = "https://www.tiktok.com/@name/video/tt-repost-1"
        ),
        Repost(
            id = "repost-2",
            tiktokPostId = "tt-repost-2",
            caption = "@Name",
            authorUsername = "@creator_b",
            likes = 830,
            durationSec = 9.0,
            createdAt = "2026-02-26T12:00:00Z",
            thumbnailUrl = "local://thumb/repost-2",
            tiktokUrl = "https://www.tiktok.com/@name/video/tt-repost-2"
        ),
        Repost(
            id = "repost-3",
            tiktokPostId = "tt-repost-3",
            caption = "@Name",
            authorUsername = "@creator_c",
            likes = 3400,
            durationSec = 16.0,
            createdAt = "2026-02-25T12:00:00Z",
            thumbnailUrl = "local://thumb/repost-3",
            tiktokUrl = "https://www.tiktok.com/@name/video/tt-repost-3"
        )
    )

    private val savedItems = mutableListOf(
        SavedItem(
            id = "saved-1",
            creatorId = creator.id,
            type = "video",
            mediaUrl = "local://saved/video-1",
            thumbnailUrl = "local://thumb/saved-1",
            createdAt = "2026-02-27T12:00:00Z"
        ),
        SavedItem(
            id = "saved-2",
            creatorId = creator.id,
            type = "image",
            mediaUrl = "local://saved/image-1",
            thumbnailUrl = "local://thumb/saved-2",
            createdAt = "2026-02-26T12:00:00Z"
        )
    )

    private val driveFiles = mutableListOf(
        ImportFile(
            id = "drive-1",
            provider = "drive",
            name = "clip_001.mp4",
            mimeType = "video/mp4",
            sizeBytes = 3_400_000,
            modifiedAt = "2026-03-04T10:00:00Z",
            downloadUrl = null
        ),
        ImportFile(
            id = "drive-2",
            provider = "drive",
            name = "clip_002.mov",
            mimeType = "video/quicktime",
            sizeBytes = 8_100_000,
            modifiedAt = "2026-03-02T14:30:00Z",
            downloadUrl = null
        )
    )

    private val dropboxFiles = mutableListOf(
        ImportFile(
            id = "dropbox-1",
            provider = "dropbox",
            name = "video_a.mp4",
            mimeType = "video/mp4",
            sizeBytes = 2_200_000,
            modifiedAt = "2026-03-03T07:20:00Z",
            downloadUrl = null
        ),
        ImportFile(
            id = "dropbox-2",
            provider = "dropbox",
            name = "video_b.mp4",
            mimeType = "video/mp4",
            sizeBytes = 5_600_000,
            modifiedAt = "2026-03-01T18:05:00Z",
            downloadUrl = null
        )
    )

    private val uploadSessions = mutableMapOf<String, UploadSession>()

    override suspend fun getMe(): Creator = creator

    override suspend fun getMyPosts(): List<Post> = posts.toList()

    override suspend fun getSavedItems(): List<SavedItem> = savedItems.toList()

    override suspend fun uploadMedia(fileBytes: ByteArray, fileName: String): MediaUpload {
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "mp4")
        val id = "media-${UUID.randomUUID()}"
        return MediaUpload(
            mediaUrl = "local://uploads/$id.$ext",
            thumbnailUrl = "local://thumb/$id",
            durationSec = 12.3
        )
    }

    override suspend fun publishPost(caption: String, mediaUrl: String, privacy: String): PublishResult {
        val newId = "post-${posts.size + 1}"
        val createdAt = Instant.now().toString()
        posts.add(
            samplePost(
                id = newId,
                caption = caption.ifBlank { "(no caption)" },
                likes = 0,
                createdAt = createdAt,
                mediaUrl = mediaUrl
            )
        )
        return PublishResult(postId = newId, status = "published")
    }

    override suspend fun updatePostCaption(id: String, caption: String): Post {
        val idx = posts.indexOfFirst { it.id == id }
        if (idx == -1) error("Post not found")
        val updated = posts[idx].copy(caption = caption)
        posts[idx] = updated
        return updated
    }

    override suspend fun deletePost(id: String) {
        posts.removeAll { it.id == id }
    }

    override suspend fun createCleanJob(postIds: List<String>): CleanJob {
        return CleanJob(
            id = "clean-${UUID.randomUUID()}",
            creatorId = creator.id,
            postIds = postIds,
            status = "done",
            progress = 1.0,
            createdAt = Instant.now().toString()
        )
    }

    override suspend fun getCleanJob(id: String): CleanJob {
        return CleanJob(
            id = id,
            creatorId = creator.id,
            postIds = emptyList(),
            status = "done",
            progress = 1.0,
            createdAt = Instant.now().toString()
        )
    }

    override suspend fun getMyReposts(): List<Repost> = reposts.toList()

    override suspend fun getMyRepostsPage(cursor: Int, count: Int): RepostPage {
        val safeCursor = cursor.coerceAtLeast(0)
        val safeCount = count.coerceAtLeast(1)
        val pageItems = reposts.drop(safeCursor).take(safeCount)
        val nextCursor = safeCursor + pageItems.size
        return RepostPage(
            items = pageItems,
            cursor = nextCursor,
            hasMore = nextCursor < reposts.size
        )
    }

    override suspend fun deleteRepost(id: String) {
        reposts.removeAll { it.id == id }
    }

    override suspend fun deleteReposts(ids: List<String>) {
        val set = ids.toSet()
        reposts.removeAll { it.id in set }
    }

    override suspend fun getDriveImportFiles(): List<ImportFile> = driveFiles.toList()

    override suspend fun getDropboxImportFiles(): List<ImportFile> = dropboxFiles.toList()

    override suspend fun ingestImportFile(provider: String, fileId: String): SavedItem {
        val id = "saved-${UUID.randomUUID()}"
        val item = SavedItem(
            id = id,
            creatorId = creator.id,
            type = "video",
            mediaUrl = "local://imports/$provider/$fileId",
            thumbnailUrl = "local://thumb/$id",
            createdAt = Instant.now().toString()
        )
        savedItems.add(0, item)
        return item
    }

    override suspend fun initUpload(fileName: String, mimeType: String, totalBytes: Long): UploadSession {
        val uploadId = "upl-${UUID.randomUUID()}"
        val session = UploadSession(
            uploadId = uploadId,
            uploadUrl = "local://uploads/$uploadId",
            chunkSizeBytes = 256 * 1024
        )
        uploadSessions[uploadId] = session
        return session
    }

    override suspend fun uploadChunk(uploadId: String, chunkIndex: Int, bytesCount: Int, crc32: Long): Boolean {
        return uploadSessions.containsKey(uploadId)
    }

    override suspend fun completeUpload(uploadId: String): MediaUpload {
        uploadSessions.remove(uploadId)
        val mediaId = "media-$uploadId"
        return MediaUpload(
            mediaUrl = "local://uploads/$mediaId.mp4",
            thumbnailUrl = "local://thumb/$mediaId",
            durationSec = 12.3
        )
    }

    private fun samplePost(
        id: String,
        caption: String,
        likes: Long,
        createdAt: String,
        mediaUrl: String = "local://media/$id.mp4"
    ): Post {
        val seed = id.hashCode().absoluteValue
        return Post(
            id = id,
            tiktokPostId = "tt-$id",
            creatorId = creator.id,
            caption = caption,
            mediaUrl = mediaUrl,
            thumbnailUrl = "local://thumb/$id",
            durationSec = 6.0 + (seed % 120) / 10.0,
            likes = likes,
            comments = (seed % 40).toLong(),
            shares = (seed % 12).toLong(),
            createdAt = createdAt,
            status = "active"
        )
    }
}

