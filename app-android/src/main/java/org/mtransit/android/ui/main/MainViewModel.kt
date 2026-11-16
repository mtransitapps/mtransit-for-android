package org.mtransit.android.ui.main

import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.commons.MTLog
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.common.SharedActivityViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    remoteConfigProvider: RemoteConfigProvider,
) : SharedActivityViewModel(remoteConfigProvider), MTLog.Loggable {

    companion object {
        private val LOG_TAG = "Stack-" + MainViewModel::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG
}
