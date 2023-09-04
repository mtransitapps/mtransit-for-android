package org.mtransit.android.ui.inappnotification.moduledisabled

import androidx.lifecycle.LiveData
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.ui.inappnotification.InAppNotificationViewModel

interface ModuleDisabledAwareViewModel : InAppNotificationViewModel {

    val moduleDisabled: LiveData<List<AgencyBaseProperties>>

    val hasDisabledModule: LiveData<Boolean>
}