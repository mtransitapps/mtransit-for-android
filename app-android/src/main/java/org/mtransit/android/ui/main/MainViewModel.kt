package org.mtransit.android.ui.main

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.common.SharedActivityViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel(
    remoteConfigProvider: RemoteConfigProvider,
    private val billingManager: IBillingManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : SharedActivityViewModel(remoteConfigProvider), MTLog.Loggable {

    @Inject
    constructor(
        remoteConfigProvider: RemoteConfigProvider,
        billingManager: IBillingManager,
        dataSourcesRepository: DataSourcesRepository,
    ) : this(
        remoteConfigProvider = remoteConfigProvider,
        billingManager = billingManager,
        dataSourcesRepository = dataSourcesRepository,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG = "Stack-" + MainViewModel::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    val hasSubscriptionOrNoError = billingManager.hasSubscriptionOrNoError

    fun onResume() {
        this.billingManager.refreshPurchases()
        viewModelScope.launch(ioDispatcher) {
            dataSourcesRepository.updateLock()
        }
    }
}
