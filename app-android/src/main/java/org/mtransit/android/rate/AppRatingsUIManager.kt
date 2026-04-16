package org.mtransit.android.rate

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.android.play.core.review.testing.FakeReviewManager
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.analytics.AnalyticsEvents
import org.mtransit.android.analytics.AnalyticsEventsParamsProvider
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog

object AppRatingsUIManager : MTLog.Loggable {

    private val LOG_TAG: String = AppRatingsUIManager::class.java.simpleName

    private const val TESTING_WITH_FAKE_REVIEW_MANAGER = false

    private const val USE_PLAY_API = true

    override fun getLogTag() = LOG_TAG

    @JvmStatic
    fun showAppRatingsUI(activity: Activity, analyticsManager: IAnalyticsManager, onAppRatingDisplayed: (Boolean) -> (Unit)) {
        MTLog.v(this, "showAppRatingsUI()")
        if (Constants.DEBUG && BuildConfig.DEBUG) {
            showFakeUI(activity, onAppRatingDisplayed)
            return
        }
        if (USE_PLAY_API) {
            showPlayInAppReviewUI(activity, analyticsManager, onAppRatingDisplayed)
        }
    }

    // region fake UI

    private fun showFakeUI(activity: Activity, onAppRatingDisplayed: (Boolean) -> (Unit)) {
        val theContextView = activity.findViewById<View>(R.id.content_frame)
        val labelText = "App Rating UI test"
        val actionText = "Rate"
        val onActionClicked: (() -> Unit) = {
            onAppRatingDisplayed(true)
        }
        Snackbar.make(theContextView, labelText, Snackbar.LENGTH_LONG).apply {
            setAction(actionText) {
                onActionClicked.invoke()
            }
        }.show()
    }

    // endregion

    // region Play In-App Review

    private var reviewManager: ReviewManager? = null

    private fun getReviewManager(context: Context?) = reviewManager ?: makeReviewManager(context).also {
        reviewManager = it
    }

    private fun makeReviewManager(context: Context?) = context?.let {
        if (TESTING_WITH_FAKE_REVIEW_MANAGER) FakeReviewManager(context) else
            ReviewManagerFactory.create(context)
    }

    private fun showPlayInAppReviewUI(activity: Activity, analyticsManager: IAnalyticsManager, onAppRatingDisplayed: (Boolean) -> Unit) {
        val manager = getReviewManager(activity) ?: return
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                manager.launchReviewFlow(activity, task.result).addOnCompleteListener { _ ->
                    // The flow has finished.
                    // The API does not indicate whether the user reviewed or not, or even whether the review dialog was shown.
                    // Thus, no matter the result, we continue our app flow.
                    onAppRatingDisplayed(true)
                }
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as? ReviewException)?.errorCode
                    ?: -1 // Huawei crash (RemoteException)
                MTLog.w(this, task.exception, "Error while requesting review flow (code: $reviewErrorCode)")
                analyticsManager.logEvent(AnalyticsEvents.APP_RATINGS_REQUEST_PLAY_ERROR, AnalyticsEventsParamsProvider().apply {
                    put(AnalyticsEvents.Params.CODE, reviewErrorCode)
                })
            }
        }
    }

    // endregion

}