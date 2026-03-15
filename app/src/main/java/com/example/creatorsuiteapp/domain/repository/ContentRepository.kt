package com.example.creatorsuiteapp.domain.repository

import com.example.creatorsuiteapp.domain.model.Creator
import com.example.creatorsuiteapp.domain.model.Post
import com.example.creatorsuiteapp.domain.model.SavedItem
import com.example.creatorsuiteapp.domain.model.CleanJob
import com.example.creatorsuiteapp.domain.model.MediaUpload
import com.example.creatorsuiteapp.domain.model.PublishResult
import com.example.creatorsuiteapp.domain.model.ImportFile
import com.example.creatorsuiteapp.domain.model.Repost
import com.example.creatorsuiteapp.domain.model.RepostPage
import com.example.creatorsuiteapp.domain.model.UploadSession

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
