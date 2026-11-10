package org.mtransit.android.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.webkit.WebView
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import java.util.Locale

@SuppressLint("DeprecatedCall")
fun interceptIntent(activity: Activity, webView: WebView, url: String, useInternalWebBrowserPref: Boolean): Boolean {
    try {
        Intent.parseUri(url, Intent.URI_INTENT_SCHEME)?.let { intent ->
            intent.apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setComponent(null)
                selector?.apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setComponent(null)
                }
                flags = flags and (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    ).inv()
            }
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                ?.takeIf {
                    val lc = it.lowercase(Locale.ROOT)
                    lc.startsWith("http://") || lc.startsWith("https://") || LinkUtils.canIntercept(it)
                }
            if (useInternalWebBrowserPref
                && fallbackUrl != null && !LinkUtils.canIntercept(fallbackUrl)
                && processFallbackUrl(activity, webView, fallbackUrl)
            ) {
                return true // INTERCEPTED
            }
            if (PackageManagerUtils.isAppInstalledDefault(webView.context, intent)) { // Only works with apps pkg added to AndroidManifest.xml (API Level 30+)
                org.mtransit.android.commons.LinkUtils.open(webView.context, intent, null)
                return true // INTERCEPTED
            }
            if (processFallbackUrl(activity, webView, fallbackUrl)) {
                return true // INTERCEPTED
            }
        }
    } catch (e: Exception) {
        MTLog.w(LinkUtils.LOG_TAG, e, "Error while processing '%s'!", url)
    }
    return false // not INTERCEPTED
}

@SuppressLint("DeprecatedCall")
private fun processFallbackUrl(activity: Activity, webView: WebView, fallbackUrl: String?): Boolean {
    return if (fallbackUrl.isNullOrEmpty()) {
        false // not INTERCEPTED
    } else if (!LinkUtils.canIntercept(fallbackUrl)) {
        webView.loadUrl(fallbackUrl)
        true // INTERCEPTED
    } else if (LinkUtils.intercept(activity, fallbackUrl)) {
        true // INTERCEPTED
    } else {
        false // not INTERCEPTED
    }
}
