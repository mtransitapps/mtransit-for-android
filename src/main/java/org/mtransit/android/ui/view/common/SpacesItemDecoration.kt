package org.mtransit.android.ui.view.common

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.commons.getDimensionInt

class SpacesItemDecoration(val space: Int) : RecyclerView.ItemDecoration() {

    constructor(
        context: Context,
        @DimenRes dimId: Int
    ) : this(
        context.resources.getDimensionInt(dimId)
    )

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space
        outRect.right = space
        outRect.bottom = space
        // add top margin only for the 1st item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}