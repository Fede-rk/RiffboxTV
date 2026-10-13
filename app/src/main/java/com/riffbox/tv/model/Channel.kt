package com.riffbox.tv.model

data class Channel(
    val id: Int,
    val name: String,
    val category: String,
    val streamUrl: String,
    val badge: String,
    val isRadio: Boolean = false,
    val logoResId: Int = 0
)
