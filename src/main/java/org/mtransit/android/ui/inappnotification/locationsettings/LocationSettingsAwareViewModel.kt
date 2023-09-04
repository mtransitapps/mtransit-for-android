package org.mtransit.android.ui.inappnotification.locationsettings

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import org.mtransit.android.ui.inappnotification.InAppNotificationViewModel

interface LocationSettingsAwareViewModel : InAppNotificationViewModel {

    val locationSettingsNeeded: LiveData<Boolean>
    val locationSettingsNeededResolution: LiveData<PendingIntent?>
}