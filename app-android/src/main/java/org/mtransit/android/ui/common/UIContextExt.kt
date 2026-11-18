package org.mtransit.android.ui.common

import android.content.Context
import org.mtransit.android.R

val Context?.twoPane: Boolean
    get() = this?.resources?.getBoolean(R.bool.two_pane) == true
