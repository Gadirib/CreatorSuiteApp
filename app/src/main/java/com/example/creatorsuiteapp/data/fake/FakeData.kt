package com.example.creatorsuiteapp.data.fake

import com.example.creatorsuiteapp.R
import com.example.creatorsuiteapp.domain.model.CleanerItem
import com.example.creatorsuiteapp.domain.model.SavedImageItem
import com.example.creatorsuiteapp.domain.model.SavedVideoItem

object FakeData {
    val cleanerItems = listOf(
        CleanerItem("post-1", "@Name", "2 days ago", "0.0k", R.drawable.sample_img_1, "00:00"),
        CleanerItem("post-2", "@Name", "2 days ago", "0.0k", R.drawable.sample_img_2, "00:00"),
        CleanerItem("post-3", "@Name", "2 days ago", "0.0k", R.drawable.sample_video_1_thumb, "00:00")
    )

    val savedVideos = listOf(
        SavedVideoItem(R.raw.sample_video_1, R.drawable.sample_video_1_thumb),
        SavedVideoItem(R.raw.sample_video_2, R.drawable.sample_video_2_thumb)
    )

    val savedImages = listOf(
        SavedImageItem(R.drawable.sample_img_1),
        SavedImageItem(R.drawable.sample_img_2)
    )
}
