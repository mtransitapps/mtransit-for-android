package org.mtransit.android.ui.inappnotification.locationpermission

import org.mtransit.android.ui.inappnotification.InAppNotificationFragment


interface LocationPermissionAwareFragment : InAppNotificationFragment {

    override val viewModel: LocationPermissionAwareViewModel
    override val attachedViewModel: LocationPermissionAwareViewModel?
}