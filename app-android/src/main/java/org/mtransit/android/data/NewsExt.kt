package org.mtransit.android.data

import android.net.Uri
import org.mtransit.android.commons.data.News
import java.util.Locale

val News.authorityT: Authority
    get() = Authority(this.authority)

val News.uuidT: Uuid
    get() = Uuid(this.uuid)

val News.authorityAndUuidT: AuthorityAndUuid
    get() = AuthorityAndUuid(this.authorityT, this.uuidT)

val News.imageUrls: List<NewsImage>
    get() = (0 until this.imageURLsCount).map {
        NewsImage(this.getImageUrl(it))
    }

const val YOUTUBE_VIDEO_URL_BEFORE_ID = "https://www.youtube.com/watch?v="

val News.isYouTubeVideo: Boolean
    get() = this.webURL.startsWith(YOUTUBE_VIDEO_URL_BEFORE_ID)

val News.youTubeVideoId: String?
    get() = this.takeIf { it.isYouTubeVideo }?.webURL?.removePrefix(YOUTUBE_VIDEO_URL_BEFORE_ID)

private const val TRUE = "1"
private const val FALSE = "0"

fun makeYouTubeEmbedVideoPlayerUrl(videoId: String, autoPlay: Boolean) = Uri.Builder().apply {
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
    appendQueryParameter("mute", if (autoPlay) TRUE else FALSE) // muted by default if auto-play
    appendQueryParameter("hl", Locale.getDefault().language) // language
}.build().toString()
