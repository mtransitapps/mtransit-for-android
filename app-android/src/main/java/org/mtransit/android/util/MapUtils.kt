package org.mtransit.android.util

import android.content.Context
import android.view.ViewGroup
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ResourceUtils

object MapUtils : MTLog.Loggable {

    private val LOG_TAG: String = MapUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private const val MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP = 64

    private var mapWithButtonsCameraPaddingInPx: Int? = null

    @JvmStatic
    fun getMapWithButtonsCameraPaddingInPx(context: Context?): Int {
        return mapWithButtonsCameraPaddingInPx
            ?: ResourceUtils.convertSPtoPX(context, MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP).toInt()
                .also { mapWithButtonsCameraPaddingInPx = it }
    }

    private const val MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP = 32

    private var mapWithoutButtonsCameraPaddingInPx: Int? = null

    @JvmStatic
    fun getMapWithoutButtonsCameraPaddingInPx(context: Context?): Int {
        return mapWithoutButtonsCameraPaddingInPx
            ?: ResourceUtils.convertSPtoPX(context, MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP).toInt()
                .also { mapWithoutButtonsCameraPaddingInPx = it }
    }

    fun fixScreenFlickering(viewGroup: ViewGroup?) {
        @Suppress("ConstantConditionIf")
        if (true) return // DISABLED (testing...)
        // https://issuetracker.google.com/issues/35822212
        viewGroup?.requestTransparentRegion(viewGroup)
    }
}
