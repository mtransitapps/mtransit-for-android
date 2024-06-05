package org.mtransit.android.ui.inappnotification.locationsettings

import org.mtransit.android.ui.inappnotification.InAppNotificationFragment


interface LocationSettingsAwareFragment : InAppNotificationFragment {

    override val viewModel: LocationSettingsAwareViewModel
    override val attachedViewModel: LocationSettingsAwareViewModel?
}