package org.mtransit.android.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.mtransit.android.commons.MTLog

/**
 * Similar to [androidx.navigation.fragment.AbstractListDetailFragment.InnerOnBackPressedCallback]
 */
class ListDetailOnBackPressedCallback(
    private val slidingPaneLayout: SlidingPaneLayout,
) : OnBackPressedCallback(enabled = true),
    SlidingPaneLayout.PanelSlideListener,
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = ListDetailOnBackPressedCallback::class.java.simpleName
    }

    init {
        slidingPaneLayout.addPanelSlideListener(this)
    }

    override fun getLogTag() = LOG_TAG

    var panelSlideListener: SlidingPaneLayout.PanelSlideListener? = null

    override fun handleOnBackPressed() {
        slidingPaneLayout.closePane()
    }

    /**
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelSlide(panel: View, slideOffset: Float) {
        // DO NOTHING
        panelSlideListener?.onPanelSlide(panel, slideOffset)
    }

    /**
     * Called when a detail view becomes slid completely open.
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelOpened(panel: View) {
        isEnabled = true
        panelSlideListener?.onPanelOpened(panel)
    }

    /**
     * Called when a detail view becomes slid completely closed.
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelClosed(panel: View) {
        isEnabled = false
        panelSlideListener?.onPanelClosed(panel)
    }
}
