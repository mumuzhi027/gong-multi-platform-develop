package com.sky31.gongmultiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class CampusActivity(
    val id: String,
    val title: String,
    val organizer: String,
    val participation: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val posterTitle: String,
    val posterSubtitle: String,
    val posterUrl: String = "",
    val images: List<String> = emptyList(),
    val description: String = "",
    val featured: Boolean = false,
    val published: Boolean = true,
    val sortOrder: Int = 0,
    val filterTags: List<String> = emptyList()
)

val previewCampusActivities = listOf(
    CampusActivity(
        id = "welcome-fair-2026",
        title = "百团招新游园会",
        organizer = "校团委 · 学生社团联合会",
        participation = "现场参与，无需报名",
        location = "南苑田径场",
        startDate = "2026.09.05",
        endDate = "2026.09.06",
        posterTitle = "百团招新",
        posterSubtitle = "找到热爱，也找到同行的人"
    ),
    CampusActivity(
        id = "campus-singer-2026",
        title = "校园十佳歌手大赛",
        organizer = "三翼校园",
        participation = "线上报名，现场海选",
        location = "学生活动中心",
        startDate = "2026.09.10",
        endDate = "2026.09.18",
        posterTitle = "声动湘大",
        posterSubtitle = "十佳歌手报名开启"
    ),
    CampusActivity(
        id = "volunteer-day-2026",
        title = "秋季校园志愿服务日",
        organizer = "青年志愿者总会",
        participation = "第二课堂报名",
        location = "琴湖广场集合",
        startDate = "2026.09.20",
        endDate = "2026.09.20",
        posterTitle = "微光成炬",
        posterSubtitle = "用一天时间，让校园更美好"
    )
)
