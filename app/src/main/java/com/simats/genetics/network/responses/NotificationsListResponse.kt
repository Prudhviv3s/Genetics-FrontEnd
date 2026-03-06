package com.simats.genetics.network.responses

import com.google.gson.annotations.SerializedName

data class NotificationsListResponse(
    val status: Boolean,
    @SerializedName("new_count")
    val newCount: Int = 0,
    val count: Int = 0,
    val notifications: List<NotificationItem> = emptyList()
)

data class NotificationItem(
    val id: Int,

    @SerializedName("notif_type")
    val notifType: String?,

    val title: String?,
    val message: String?,

    @SerializedName("is_read")
    val isRead: Boolean = false,

    // backend may return created_at; if not, you can ignore
    @SerializedName("created_at")
    val createdAt: String? = null
)