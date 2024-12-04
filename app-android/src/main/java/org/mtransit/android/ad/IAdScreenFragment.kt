package org.mtransit.android.ad

import org.mtransit.android.ui.view.common.IFragment

interface IAdScreenFragment : IFragment {

    fun hasAds(): Boolean = false
}