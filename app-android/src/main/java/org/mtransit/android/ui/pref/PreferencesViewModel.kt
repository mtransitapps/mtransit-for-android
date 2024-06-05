package org.mtransit.android.ui.pref

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = PreferencesViewModel::class.java.simpleName

        internal const val EXTRA_SUPPORT = "extra_support"
    }

    override fun getLogTag(): String = LOG_TAG

    val showSupport = savedStateHandle.getLiveDataDistinct(EXTRA_SUPPORT, false)

    fun onSupportShown() {
        savedStateHandle[EXTRA_SUPPORT] = false
    }
}