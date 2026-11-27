package org.mtransit.android.ui.view

import androidx.core.view.isVisible
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.JPaths

fun MTJPathsView.setJsonAndVisibility(agency: IAgencyUIProperties?) =
    setJsonAndVisibility(agency?.logo, agency?.authority)

fun MTJPathsView.setJsonAndVisibility(jPaths: JPaths?, tag: String?) {
    if (hasPaths() && tag == this.tag) {
        isVisible = true
        return
    }
    jPaths?.let {
        setJson(it)
        this.tag = tag
        isVisible = true
    } ?: run {
        isVisible = false
    }
}
