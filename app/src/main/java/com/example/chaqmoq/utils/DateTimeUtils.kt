package com.example.news.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    fun getRelativeTime(publishedAt: String): CharSequence {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val time = dateFormat.parse(publishedAt)?.time ?: return ""
        return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
    }
}
