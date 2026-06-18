package org.mtransit.android.ui.view

import androidx.core.view.isVisible
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.JPaths

fun MTJPathsView.setJSONAndVisibility(agency: IAgencyUIProperties?) =
    setJSONAndVisibility(agency?.logo, agency?.authority)

fun MTJPathsView.setJSONAndVisibility(jPaths: JPaths?, tag: String?) {
    if (hasPaths() && tag == this.tag) {
        isVisible = true
        return
    }
    jPaths?.let {
        setJSON(it)
        this.tag = tag
        isVisible = true
    } ?: run {
        isVisible = false
    }
}
