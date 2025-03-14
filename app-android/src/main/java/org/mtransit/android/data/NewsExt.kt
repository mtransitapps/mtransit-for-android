package org.mtransit.android.data

import android.net.Uri
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.TwitterNewsProvider
import org.mtransit.android.commons.provider.YouTubeNewsProvider
import java.util.Locale

@Suppress("KotlinConstantConditions")
val News.uniqueId: Long
    get() {
        var result = 0L
        result = 31 * result + this.uuid.hashCode()
        return result
    }

val News.authorityT: Authority
    get() = this.authority

val News.uuidT: Uuid
    get() = this.uuid

val News.authorityAndUuidT: AuthorityAndUuid
    get() = AuthorityAndUuid(this.authorityT, this.uuidT)

val News.authorWithUserName: String
    get() = buildString {
        append(authorName)
        authorUsername?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
    }

val News.sourceLabelWithUserName: String
    get() = buildString {
        authorUsername?.takeIf { it.isNotBlank() }?.let { append("$it - ") }
        append(sourceLabel)
    }

val News.imageUrls: List<NewsImage>
    get() = (0 until this.imageURLsCount).map {
        NewsImage(this.getImageUrl(it))
    }

val News.hasVideo: Boolean
    get() = isTwitterVideo || isYouTubeVideo

val News.hasImagesOrVideoThumbnail: Boolean
    get() = this.imageUrls.isNotEmpty() || isTwitterVideo || isYouTubeVideo

// region Twitter

val News.isTwitterVideo: Boolean
    get() = this.imageUrls.any { it.isTwitterVideo }

val NewsImage.isTwitterVideo: Boolean
    get() = this.imageUrl.contains("video_thumb")

fun News.getTwitterVideoId(): String? { // tweet ID
    return this.takeIf { it.isTwitterVideo }?.webURL?.let {
        TwitterNewsProvider.WEB_URL_REGEX.replace(
            it,
            "$" + TwitterNewsProvider.WEB_URL_REGEX_GROUP_TWEET_ID
        )
    }
}

fun makeTwitterEmbedVideoPlayerUrl(videoId: String) = Uri.Builder().apply {
    scheme("https")
    authority("platform.twitter.com")
    appendPath("embed")
    appendPath("Tweet.html")
    appendQueryParameter("dnt", "false")
    appendQueryParameter("frame", "false")
    appendQueryParameter("cards", "false")
    appendQueryParameter("hideCard", "true")
    appendQueryParameter("hideThread", "true")
    appendQueryParameter("lang", Locale.getDefault().language)
    appendQueryParameter("maxWidth", "1920px")
    appendQueryParameter("id", videoId)
}.build().toString()

// endregion Twitter

// region YouTube

val News.isYouTubeVideo: Boolean
    get() = this.webURL.startsWith(YouTubeNewsProvider.YOUTUBE_VIDEO_URL_BEFORE_ID)

fun News.getYouTubeVideoId(): String? = this.takeIf { it.isYouTubeVideo }?.webURL?.removePrefix(YouTubeNewsProvider.YOUTUBE_VIDEO_URL_BEFORE_ID)

private const val TRUE = "1"
private const val FALSE = "0"

fun makeYouTubeEmbedVideoPlayerUrl(videoId: String, autoPlay: Boolean, mute: Boolean = autoPlay) = Uri.Builder().apply {
    scheme("https")
    authority("www.youtube.com")
    appendPath("embed")
    appendPath(videoId)
    appendQueryParameter("autoplay", if (autoPlay) TRUE else FALSE)
    appendQueryParameter("fs", FALSE) // full screen
    appendQueryParameter("controls", TRUE) // needs native control
    appendQueryParameter("modestbranding", TRUE) // deprecated (less YouTube branding)
    appendQueryParameter("rel", FALSE) // only related videos from same channel
    appendQueryParameter("showinfo", FALSE) // deprecated (no video title)
    appendQueryParameter("disablekb", FALSE) // keyboard control
    appendQueryParameter("mute", if (mute) TRUE else FALSE) // muted by default if auto-play
    appendQueryParameter("hl", Locale.getDefault().language) // language
}.build().toString()

// endregion YouTube