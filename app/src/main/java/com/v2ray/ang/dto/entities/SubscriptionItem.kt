package com.v2ray.ang.dto.entities

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,
    val addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = false,
    var updateInterval: Long = 1440, // in minutes, default to 24 hours
    var prevProfile: String? = null,
    var nextProfile: String? = null,
    var filter: String? = null,
    var allowInsecureUrl: Boolean = false,
    var userAgent: String? = null,
    var description: String? = null,
    var announce: String? = null,
    var supportUrl: String? = null,
    var webPageUrl: String? = null,
    var announceUrl: String? = null,
    var uploadBytes: Long? = null,
    var downloadBytes: Long? = null,
    var totalBytes: Long? = null,
    var expireAt: Long? = null,
)

