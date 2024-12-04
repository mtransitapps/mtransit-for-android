package org.mtransit.android.ui.inappnotification

import org.mtransit.android.ad.IAdScreenActivity

interface InAppNotificationViewModel {

    fun getAdBannerHeightInPx(activity: IAdScreenActivity?): Int?

}