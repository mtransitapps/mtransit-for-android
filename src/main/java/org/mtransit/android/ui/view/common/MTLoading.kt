package org.mtransit.android.ui.view.common

import androidx.annotation.FloatRange

const val NOT_LOADING: Float = -1.0f

const val LOADING_PROGRESS_UNKNOWN: Float = 0.0f

const val LOADED: Float = 1.0f

@Suppress("unused")
data class MTLoading(
    @FloatRange(from = -1.0, to = 1.0) val percent: Float
) {
    constructor(loading: Boolean) : this(
        if (loading) {
            LOADING_PROGRESS_UNKNOWN
        } else {
            NOT_LOADING
        }
    )

    val loading: Boolean = percent >= LOADING_PROGRESS_UNKNOWN && percent < LOADED

    val loaded = percent == LOADED
    val notStarted = percent < LOADING_PROGRESS_UNKNOWN
}