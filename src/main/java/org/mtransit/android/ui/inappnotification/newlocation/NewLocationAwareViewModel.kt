package org.mtransit.android.ui.inappnotification.newlocation

import androidx.lifecycle.LiveData
import org.mtransit.android.ui.inappnotification.InAppNotificationViewModel

interface NewLocationAwareViewModel : InAppNotificationViewModel {

    val newLocationAvailable: LiveData<Boolean?>
    fun initiateRefresh(): Boolean
}