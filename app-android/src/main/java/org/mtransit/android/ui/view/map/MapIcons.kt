package org.mtransit.android.ui.view.map

import android.content.Context
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.scale
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.mtransit.android.commons.ColorUtils

object MapIcons {

    @JvmStatic
    fun resetColorCache() {
        cache.evictAll()
    }

    private val cache = LruCache<Triple<Int, Int, Int?>, BitmapDescriptor>(128)

    @JvmStatic
    fun getIcon(context: Context?, @DrawableRes iconResId: Int, @ColorInt color: Int, replaceColor: Boolean, size: Int? = null): BitmapDescriptor? {
        val cacheKey = Triple(iconResId, color, size)
        cache.get(cacheKey)?.let { cachedBitmap ->
            return cachedBitmap
        }
        val themeColor = ColorUtils.adaptColorToTheme(context, color)
        val newBase = ColorUtils.colorizeBitmapResource(context, themeColor, iconResId, replaceColor)
            ?.let { bitmap ->
                size?.let { bitmap.scale(it, it) } ?: bitmap
            } ?: return null // not bitmap loaded
        return BitmapDescriptorFactory.fromBitmap(newBase).also { newBitmapDescriptor ->
            cache.put(cacheKey, newBitmapDescriptor)
        }
    }
}
