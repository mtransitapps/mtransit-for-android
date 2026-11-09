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
                selector?.setComponent(null)
            }
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                ?.takeIf {
                    val lc = it.lowercase(Locale.ROOT)
                    lc.startsWith("http://") || lc.startsWith("https://") || LinkUtils.canIntercept(it)
                }
            if (useInternalWebBrowserPref && processFallbackUrl(activity, webView, fallbackUrl)) {
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

private fun processFallbackUrl(activity: Activity, webView: WebView, fallbackUrl: String?): Boolean {
    if (!fallbackUrl.isNullOrEmpty()) {
        if (!LinkUtils.canIntercept(fallbackUrl)) {
            webView.loadUrl(fallbackUrl)
            return true // INTERCEPTED
        } else if (LinkUtils.intercept(activity, fallbackUrl)) {
            return true // INTERCEPTED
        }
    }
    return false; // not INTERCEPTED
}
