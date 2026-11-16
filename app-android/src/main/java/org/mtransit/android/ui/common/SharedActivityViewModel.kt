package org.mtransit.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider

@Suppress("AbstractClassCanBeConcreteClass")
abstract class SharedActivityViewModel(
    private val remoteConfigProvider: RemoteConfigProvider,
) : ViewModel() {

    fun onCreate() {
        viewModelScope.launch {
            remoteConfigProvider.init()
        }
    }
}
