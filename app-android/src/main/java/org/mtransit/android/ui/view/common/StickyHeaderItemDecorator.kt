package org.mtransit.android.ui.view.common

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.core.graphics.withTranslation

class StickyHeaderItemDecorator<VH : RecyclerView.ViewHolder>(
    private val adapter: StickyAdapter<VH>,
    private val recyclerView: RecyclerView,
) : ItemDecoration() {
    private var currentStickyPosition = RecyclerView.NO_POSITION
    private val currentStickyHolder: VH = adapter.onCreateHeaderViewHolder(recyclerView)
    private var lastViewOverlappedByHeader: View? = null

    init {
        fixLayoutSize()
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val layoutManager = parent.layoutManager ?: return
        var topChildPosition = RecyclerView.NO_POSITION
        if (layoutManager is LinearLayoutManager) {
            topChildPosition = layoutManager.findFirstVisibleItemPosition()
        } else {
            val topChild = parent.getChildAt(0)
            if (topChild != null) {
                topChildPosition = parent.getChildAdapterPosition(topChild)
            }
        }
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }
        val viewOverlappedByHeader = getChildInContact(parent, currentStickyHolder.itemView.bottom)
            ?: lastViewOverlappedByHeader
            ?: parent.getChildAt(topChildPosition)
            ?: return // !
        lastViewOverlappedByHeader = viewOverlappedByHeader
        val overlappedByHeaderPosition = parent.getChildAdapterPosition(viewOverlappedByHeader)
        val overlappedHeaderPosition: Int
        val preOverlappedPosition: Int
        if (overlappedByHeaderPosition > 0) {
            preOverlappedPosition = adapter.getHeaderPositionForItem(overlappedByHeaderPosition - 1)
            overlappedHeaderPosition = adapter.getHeaderPositionForItem(overlappedByHeaderPosition)
        } else {
            preOverlappedPosition = adapter.getHeaderPositionForItem(topChildPosition)
            overlappedHeaderPosition = preOverlappedPosition
        }
        if (preOverlappedPosition == RecyclerView.NO_POSITION) {
            return
        }
        if (preOverlappedPosition != overlappedHeaderPosition
            && shouldMoveHeader(viewOverlappedByHeader)
        ) {
            updateStickyHeader(topChildPosition)
            moveHeader(c, viewOverlappedByHeader)
        } else {
            updateStickyHeader(topChildPosition)
            drawHeader(c)
        }
    }

    private fun shouldMoveHeader(viewOverlappedByHeader: View): Boolean {
        val dy = viewOverlappedByHeader.top - viewOverlappedByHeader.height
        return viewOverlappedByHeader.top >= 0 && dy <= 0
    }

    private fun updateStickyHeader(topChildPosition: Int) {
        val headerPositionForItem = adapter.getHeaderPositionForItem(topChildPosition)
        if (headerPositionForItem == RecyclerView.NO_POSITION) {
            return
        }
        if (currentStickyPosition != headerPositionForItem) {
            adapter.onBindHeaderViewHolder(currentStickyHolder, headerPositionForItem)
            currentStickyPosition = headerPositionForItem
        }
    }

    private fun drawHeader(c: Canvas) {
        c.withTranslation(0f, 0f) {
            currentStickyHolder.itemView.draw(this)
        }
    }

    private fun moveHeader(c: Canvas, nextHeader: View) {
        c.withTranslation(0f, (nextHeader.top - nextHeader.height).toFloat()) {
            currentStickyHolder.itemView.draw(this)
        }
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        return (0 until parent.childCount).map { index ->
            parent.getChildAt(index)
        }.firstOrNull { child ->
            child.bottom > contactPoint && contactPoint >= child.top
        }
    }

    private fun fixLayoutSize() {
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val recyclerViewWidthSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.width, View.MeasureSpec.EXACTLY)
                val recyclerViewHeightSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.height, View.MeasureSpec.UNSPECIFIED)
                val childWidthSpec = ViewGroup.getChildMeasureSpec(
                    recyclerViewWidthSpec,
                    recyclerView.paddingLeft + recyclerView.paddingRight,
                    currentStickyHolder.itemView.layoutParams.width
                )
                val childHeightSpec = ViewGroup.getChildMeasureSpec(
                    recyclerViewHeightSpec,
                    recyclerView.paddingTop + recyclerView.paddingBottom,
                    currentStickyHolder.itemView.layoutParams.height
                )
                currentStickyHolder.itemView.measure(childWidthSpec, childHeightSpec)
                currentStickyHolder.itemView.layout(
                    0, 0,
                    currentStickyHolder.itemView.measuredWidth,
                    currentStickyHolder.itemView.measuredHeight
                )
            }
        })
    }

    interface StickyAdapter<VH : RecyclerView.ViewHolder> {

        fun getHeaderPositionForItem(itemPosition: Int): Int

        fun onBindHeaderViewHolder(holder: VH, headerPosition: Int)

        fun onCreateHeaderViewHolder(parent: ViewGroup): VH
    }
}