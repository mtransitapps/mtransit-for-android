package org.mtransit.android.ui.inappnotification

import org.mtransit.android.ui.view.common.IActivity

interface InAppNotificationViewModel {

    fun getAdBannerHeightInPx(activity: IActivity?): Int?

}