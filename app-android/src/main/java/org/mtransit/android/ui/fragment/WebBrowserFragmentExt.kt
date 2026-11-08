package org.mtransit.android.ui.fragment

import androidx.core.net.toUri

private const val TEXT_HTML_MIME_TYPE = "text/html"
private const val UTF_8_ENCODING = "utf-8"

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
                    allowfullscreen
                    src="$frameUrl">
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
