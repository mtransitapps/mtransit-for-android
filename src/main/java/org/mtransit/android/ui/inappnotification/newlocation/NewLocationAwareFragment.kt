package org.mtransit.android.ui.inappnotification.newlocation

import org.mtransit.android.ui.inappnotification.InAppNotificationFragment


interface NewLocationAwareFragment : InAppNotificationFragment {

    override val viewModel: NewLocationAwareViewModel
    override val attachedViewModel: NewLocationAwareViewModel?
}