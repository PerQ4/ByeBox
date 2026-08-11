package com.perqa.byebox.data

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionSource(
    val id: String,
    val name: String,
    val url: String,
    val lastUpdatedAt: Long? = null,
    val nodeCount: Int = 0,
    val uploadBytes: Long? = null,
    val downloadBytes: Long? = null,
    val totalBytes: Long? = null,
    val expireAt: Long? = null,
    val description: String? = null
)
