package org.mtransit.android.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MainThread
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.mtransit.android.commons.MTLog

class TwoPaneOnBackPressedCallback(
    private val slidingPaneLayout: SlidingPaneLayout,
    private val onPanelHandledBackPressedCallback: () -> Unit,
    private val onPanelOpenedCallback: () -> Unit,
    private val onPanelClosedCallback: () -> Unit,
) : OnBackPressedCallback(enabled = false),
    SlidingPaneLayout.PanelSlideListener,
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = TwoPaneOnBackPressedCallback::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    @MainThread
    fun setEnabledState(enabled: Boolean = slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen) {
        isEnabled = enabled
    }

    override fun handleOnBackPressed() {
        slidingPaneLayout.closePane()
        onPanelHandledBackPressedCallback()
    }

    /**
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelSlide(panel: View, slideOffset: Float) {
        // DO NOTHING
    }

    /**
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelOpened(panel: View) {
        isEnabled = true
        onPanelOpenedCallback()
    }

    /**
     * @param panel view can actually be null in real-life and crash if `init()` called too soon
     */
    override fun onPanelClosed(panel: View) {
        isEnabled = false
        onPanelClosedCallback()
    }
}
