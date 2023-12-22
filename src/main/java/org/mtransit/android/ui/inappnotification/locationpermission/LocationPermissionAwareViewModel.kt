package org.mtransit.android.ui.inappnotification.locationpermission

import androidx.lifecycle.LiveData
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.inappnotification.InAppNotificationViewModel

interface LocationPermissionAwareViewModel : InAppNotificationViewModel {

    val locationPermissionNeeded: LiveData<Boolean>

    val onboarding: LiveData<Boolean>

    fun refreshLocationPermissionNeeded()

    fun enableLocationPermission(activity: MTActivityWithLocation)
}