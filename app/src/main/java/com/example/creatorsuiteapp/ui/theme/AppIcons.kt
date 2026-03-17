package com.example.creatorsuiteapp.ui.theme

import com.example.creatorsuiteapp.R




object AppIcons {

    val CreatorLogo     = R.drawable.creator_logo
    val TikTok          = R.drawable.ic_tiktok
    val TikTokBadge     = R.drawable.ic_tiktok_badge

    val Settings        = R.drawable.ic_settings
    val Search          = R.drawable.ic_search
    val MenuDots        = R.drawable.ic_menu_dots
    val Expand          = R.drawable.ic_expand

    val RecordVideo     = R.drawable.ic_record_video
    val EditContent     = R.drawable.ic_edit_content
    val PostTikTok      = R.drawable.ic_post_tiktok
    val AddEffects      = R.drawable.ic_add_effects
    val CleanFeed       = R.drawable.ic_clean_feed
    val Share           = R.drawable.ic_share
    val Delete          = R.drawable.ic_delete
    val Files           = R.drawable.ic_files
    val VideoSmall      = R.drawable.ic_video_small

    val TrimTool        = R.drawable.ic_trim_tool
    val SizeTool        = R.drawable.ic_size_tool
    val TextTool        = R.drawable.ic_text_tool
    val StickerTool     = R.drawable.ic_sticker_tool

    val RecFilter       = R.drawable.ic_rec_filter
    val RecBg           = R.drawable.ic_rec_bg
    val RecFx           = R.drawable.ic_rec_fx
    val RecAudio        = R.drawable.ic_rec_audio

    val CleanerSearch   = R.drawable.ic_cleaner_search
    val CleanerRefresh  = R.drawable.ic_cleaner_refresh
    val CleanerSelectAll = R.drawable.ic_cleaner_select_all
    val CleanerDone     = R.drawable.ic_cleaner_done
    val CleanerEmpty    = R.drawable.ic_cleaner_empty
    val CleanerDays     = R.drawable.ic_cleaner_days

    val Heart           = R.drawable.ic_heart
    val User22          = R.drawable.ic_user_22
    val SampleImg1      = R.drawable.sample_img_1
}


object AppStickers {


    val Pack1 = listOf(
        R.drawable.sticker_0,
        R.drawable.sticker_1,
        R.drawable.sticker_2,
        R.drawable.sticker_3,
        R.drawable.sticker_4,
        R.drawable.sticker_5,
        R.drawable.sticker_6,
        R.drawable.sticker_7,
        R.drawable.sticker_8,
        R.drawable.sticker_9,
    )

    val Pack2 = listOf(
        R.drawable.sticker_10,
        R.drawable.sticker_11,
        R.drawable.sticker_12,
        R.drawable.sticker_13,
        R.drawable.sticker_14,
        R.drawable.sticker_15,
        R.drawable.sticker_16,
        R.drawable.sticker_17,
        R.drawable.sticker_18,
        R.drawable.sticker_19,
    )

    val Pack3 = listOf(
        R.drawable.sticker_20,
        R.drawable.sticker_21,
        R.drawable.sticker_22,
        R.drawable.sticker_23,
        R.drawable.sticker_24,
        R.drawable.sticker_25,
        R.drawable.sticker_26,
        R.drawable.sticker_27,
        R.drawable.sticker_28,
        R.drawable.sticker_29,
    )

    val Pack4 = listOf(
        R.drawable.sticker_30,
        R.drawable.sticker_31,
        R.drawable.sticker_32,
        R.drawable.sticker_33,
        R.drawable.sticker_34,
        R.drawable.sticker_35,
        R.drawable.sticker_36,
        R.drawable.sticker_37,
        R.drawable.sticker_38,
        R.drawable.sticker_39,
    )

    val Pack5 = listOf(
        R.drawable.sticker_40,
        R.drawable.sticker_41,
        R.drawable.sticker_42,
        R.drawable.sticker_43,
        R.drawable.sticker_44,
        R.drawable.sticker_45,
        R.drawable.sticker_46,
        R.drawable.sticker_47,
        R.drawable.sticker_48,
    )

    val packNames = listOf("Pack 1", "Pack 2", "Pack 3", "Pack 4", "Pack 5")

    val all = mapOf(
        "Pack 1" to Pack1,
        "Pack 2" to Pack2,
        "Pack 3" to Pack3,
        "Pack 4" to Pack4,
        "Pack 5" to Pack5,
    )

    
    fun forPack(packName: String): List<Int> = all[packName] ?: Pack1
}