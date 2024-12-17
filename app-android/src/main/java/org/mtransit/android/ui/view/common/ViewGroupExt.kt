package org.mtransit.android.ui.view.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(@LayoutRes resource: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(this.context).inflate(resource, this, attachToRoot)
}

var ViewGroup.MarginLayoutParams.startMargin: Int
    get() = this.marginStart
    set(value) {
        setMarginStart(value)
    }

var ViewGroup.MarginLayoutParams.endMargin: Int
    get() = this.marginEnd
    set(value) {
        setMarginEnd(value)
    }
