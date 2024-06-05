package org.mtransit.android.ui.view.common

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

fun RecyclerView.scrollToPositionWithOffset(position:Int, offset: Int) {
    when(this.layoutManager) {
        is GridLayoutManager -> (this.layoutManager as GridLayoutManager).scrollToPositionWithOffset(position, offset)
        is LinearLayoutManager -> (this.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)
        is StaggeredGridLayoutManager -> (this.layoutManager as StaggeredGridLayoutManager).scrollToPositionWithOffset(position, offset)
        else -> TODO("$this no supported!")
    }
}