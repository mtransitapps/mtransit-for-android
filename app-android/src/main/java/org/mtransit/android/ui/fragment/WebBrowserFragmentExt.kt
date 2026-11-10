package org.mtransit.android.ui.fragment

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.core.net.toUri
import androidx.core.text.htmlEncode

private const val TEXT_HTML_MIME_TYPE = "text/html"
private const val UTF_8_ENCODING = "utf-8"

@JvmOverloads
fun String.urlMatch(otherUrl: String?, @FloatRange(0.00, 1.00) min: Float = .75f): Boolean {
    otherUrl ?: return false
    if (this == otherUrl) return true
    if (min >= 1.00f) return false
    val thisUri = runCatching { this.toUri() }.getOrNull() ?: return false
    val otherUri = runCatching { otherUrl.toUri() }.getOrNull() ?: return false
    if (thisUri.scheme != otherUri.scheme || thisUri.host != otherUri.host || thisUri.port != otherUri.port) return false
    // ELSE it's good enough to get common [pre/suf]fix to approximate
    // ex: Google Maps directions URLs might only differ by GPS lat/lng slight modification -> should process as match
    fun restAfterAuthority(url: String): String? {
        val authorityStart = url.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: return null
        val restStart = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
        return url.substring(if (restStart >= 0) restStart else url.length)
    }
    val thisUrlRest = restAfterAuthority(this)?.removeSuffix("/") ?: return false
    val otherUrlRest = restAfterAuthority(otherUrl)?.removeSuffix("/") ?: return false
    if (thisUrlRest.isEmpty()) return otherUrlRest.isEmpty()
    val commonPrefixWith = thisUrlRest.commonPrefixWith(otherUrlRest)
    val commonSuffixWith = thisUrlRest.commonSuffixWith(otherUrlRest)
    val maxLength = maxOf(thisUrlRest.length, otherUrlRest.length)
    val minLength = minOf(thisUrlRest.length, otherUrlRest.length)
    val match = (commonPrefixWith.length + commonSuffixWith.length).coerceAtMost(minLength).toFloat() / maxLength
    return match > min
}

fun WebBrowserFragment.isIFrameBaseUrl(url: String): Boolean {
    val originallyUseIFrame = BundleUtils.getBoolean(EXTRA_USE_IFRAME, arguments) ?: false
    if (!originallyUseIFrame) return false
    val initialFrameUrl = this.initialUrl?.takeIf { it.isNotBlank() } ?: return false
    val baseUrl = extractBaseUrl(initialFrameUrl) ?: return false
    return url.startsWith(baseUrl)
}

@SuppressLint("DeprecatedCall")
fun WebBrowserFragment.loadIFrame(): Boolean {
    val webView = this.binding?.webView ?: return false
    val iFrameUrl = this.initialUrl?.takeIf { it.isNotBlank() } ?: return false
    val baseUrl = extractBaseUrl(iFrameUrl) ?: return false

    webView.loadDataWithBaseURL(
        baseUrl,
        makeIFrameHTML(iFrameUrl),
        TEXT_HTML_MIME_TYPE,
        UTF_8_ENCODING,
        null
    )
    return true
}

private fun makeIFrameHTML(frameUrl: String): String {
    @Suppress("HtmlRequiredLangAttribute", "SpellCheckingInspection")
    return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe
                    frameborder="0"
                    style="border:0"
                    referrerpolicy="no-referrer-when-downgrade"
                    loading="lazy"
                    allow="geolocation"
                    allowfullscreen
                    src="${frameUrl.htmlEncode()}">
                </iframe>
            </body>
            </html>
    """.trimIndent()
}

private fun extractBaseUrl(fullUrl: String): String? {
    val uri = fullUrl.toUri()
    if (uri.scheme == null || uri.host == null) return null

    val portPart = uri.port.takeIf { it != -1 }?.let { ":$it" }.orEmpty()
    return "${uri.scheme}://${uri.host}$portPart/"
}
