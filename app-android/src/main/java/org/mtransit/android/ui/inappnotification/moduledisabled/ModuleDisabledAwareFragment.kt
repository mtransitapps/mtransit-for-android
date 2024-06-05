package org.mtransit.android.ui.inappnotification.moduledisabled

import org.mtransit.android.ui.inappnotification.InAppNotificationFragment


interface ModuleDisabledAwareFragment : InAppNotificationFragment {

    override val viewModel: ModuleDisabledAwareViewModel
    override val attachedViewModel: ModuleDisabledAwareViewModel?
}