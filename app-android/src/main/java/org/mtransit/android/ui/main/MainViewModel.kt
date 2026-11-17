package org.mtransit.android.ui.main

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.common.SharedActivityViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    remoteConfigProvider: RemoteConfigProvider,
    private val billingManager: IBillingManager,
    private val dataSourcesRepository: DataSourcesRepository,
) : SharedActivityViewModel(remoteConfigProvider), MTLog.Loggable {

    companion object {
        private val LOG_TAG = "Stack-" + MainViewModel::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    val hasSubscriptionOrNoError = billingManager.hasSubscriptionOrNoError

    fun onResume() {
        this.billingManager.refreshPurchases()
        viewModelScope.launch(Dispatchers.IO) {
            dataSourcesRepository.updateLock()
        }
    }
}
