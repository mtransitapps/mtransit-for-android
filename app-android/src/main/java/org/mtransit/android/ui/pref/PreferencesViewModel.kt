package org.mtransit.android.ui.pref

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.common.SharedActivityViewModel
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    remoteConfigProvider: RemoteConfigProvider,
) : SharedActivityViewModel(remoteConfigProvider), MTLogK.Loggable {

    companion object {
        private val LOG_TAG: String = PreferencesViewModel::class.java.simpleName

        internal const val EXTRA_SUPPORT = "extra_support"
    }

    override fun getLogTag() = LOG_TAG

    val showSupport = savedStateHandle.getLiveDataDistinct(EXTRA_SUPPORT, false)

    fun onSupportShown() {
        savedStateHandle[EXTRA_SUPPORT] = false
    }
}
