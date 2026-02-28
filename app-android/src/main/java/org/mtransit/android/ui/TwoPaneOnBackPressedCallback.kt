package org.mtransit.android.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import org.mtransit.android.commons.MTLog

class TwoPaneOnBackPressedCallback(
    private val slidingPaneLayout: SlidingPaneLayout,
    private val onPanelHandledBackPressedCallback: () -> Unit,
    private val onPanelOpenedCallback: () -> Unit,
    private val onPanelClosedCallback: () -> Unit,
) : OnBackPressedCallback(
    slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
), SlidingPaneLayout.PanelSlideListener, MTLog.Loggable {

    companion object {
        private val LOG_TAG = OnBackPressedCallback::class.java.simpleName
    }

    init {
        slidingPaneLayout.addPanelSlideListener(this)
    }

    override fun getLogTag(): String = LOG_TAG

    override fun handleOnBackPressed() {
        slidingPaneLayout.closePane()
        onPanelHandledBackPressedCallback()
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
    }

    override fun onPanelOpened(panel: View?) {
        isEnabled = true
        onPanelOpenedCallback()
    }

    override fun onPanelClosed(panel: View?) {
        isEnabled = false
        onPanelClosedCallback()
    }
}