package org.mtransit.android.data

import android.view.View
import androidx.annotation.DrawableRes

interface POIListFooterManager {

    val isShowLoading: Boolean get() = false

    val isShowText: Boolean get() = false

    val text: String? get() = null

    @get:DrawableRes
    val textStartDrawableRes: Int? get() = null

    val onTextClickListener: View.OnClickListener? get() = null
}
