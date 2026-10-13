package com.riffbox.tv.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.riffbox.tv.model.Channel

object AnalyticsManager {

    private const val TAG = "AnalyticsManager"
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context, isTvDevice: Boolean) {
        try {
            analytics = Firebase.analytics
            val deviceType = if (isTvDevice) "android_tv" else "mobile"
            analytics?.setUserProperty("device_type", deviceType)
            Log.d(TAG, "Firebase Analytics initialized successfully for $deviceType")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics init error: ${e.message}")
        }
    }

    fun logChannelPlayed(channel: Channel) {
        try {
            val bundle = Bundle().apply {
                putInt("channel_id", channel.id)
                putString(FirebaseAnalytics.Param.ITEM_ID, channel.id.toString())
                putString(FirebaseAnalytics.Param.ITEM_NAME, channel.name)
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, channel.category)
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, if (channel.isRadio) "radio" else "tv")
                putBoolean("is_radio", channel.isRadio)
            }
            analytics?.logEvent("channel_play", bundle)
            analytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        } catch (e: Exception) {
            Log.w(TAG, "Log event error: ${e.message}")
        }
    }

    fun logCategorySelected(category: String) {
        try {
            val bundle = Bundle().apply {
                putString("category_name", category)
            }
            analytics?.logEvent("category_select", bundle)
        } catch (e: Exception) {
            Log.w(TAG, "Log category error: ${e.message}")
        }
    }
}