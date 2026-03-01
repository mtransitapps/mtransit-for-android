package org.mtransit.android.ui.purchase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.Event
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    billingManager: IBillingManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = PurchaseViewModel::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    val closeEvent = MutableLiveData<Event<Boolean>>()

}