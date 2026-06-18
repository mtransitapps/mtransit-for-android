package org.mtransit.android.data

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.Px

interface POIListFooterManager {

    val isShowLoading: Boolean get() = false

    val isShowText: Boolean get() = false

    val text: CharSequence? get() = null

    @get:Px
    val textHorizontalMargin: Int get() = 0

    @get:DrawableRes
    val textStartDrawableRes: Int? get() = null

    val onTextClickListener: View.OnClickListener? get() = null
}
