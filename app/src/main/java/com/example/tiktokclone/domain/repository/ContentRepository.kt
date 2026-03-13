package com.example.tiktokclone.domain.repository

import com.example.tiktokclone.domain.model.Creator
import com.example.tiktokclone.domain.model.Post
import com.example.tiktokclone.domain.model.SavedItem
import com.example.tiktokclone.domain.model.CleanJob
import com.example.tiktokclone.domain.model.MediaUpload
import com.example.tiktokclone.domain.model.PublishResult
import com.example.tiktokclone.domain.model.ImportFile
import com.example.tiktokclone.domain.model.Repost
import com.example.tiktokclone.domain.model.RepostPage
import com.example.tiktokclone.domain.model.UploadSession

interface ContentRepository {
    suspend fun getMe(): Creator
    suspend fun getMyPosts(): List<Post>
    suspend fun getSavedItems(): List<SavedItem>
    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String): MediaUpload
    suspend fun publishPost(caption: String, mediaUrl: String, privacy: String): PublishResult
    suspend fun updatePostCaption(id: String, caption: String): Post
    suspend fun deletePost(id: String)
    suspend fun createCleanJob(postIds: List<String>): CleanJob
    suspend fun getCleanJob(id: String): CleanJob
    suspend fun getMyReposts(): List<Repost>
    suspend fun getMyRepostsPage(cursor: Int, count: Int = 30): RepostPage
    suspend fun deleteRepost(id: String)
    suspend fun deleteReposts(ids: List<String>)
    suspend fun getDriveImportFiles(): List<ImportFile>
    suspend fun getDropboxImportFiles(): List<ImportFile>
    suspend fun ingestImportFile(provider: String, fileId: String): SavedItem
    suspend fun initUpload(fileName: String, mimeType: String, totalBytes: Long): UploadSession
    suspend fun uploadChunk(uploadId: String, chunkIndex: Int, bytesCount: Int, crc32: Long): Boolean
    suspend fun completeUpload(uploadId: String): MediaUpload
}
