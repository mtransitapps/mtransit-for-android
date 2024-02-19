package org.mtransit.android.rate

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.android.play.core.review.testing.FakeReviewManager
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.IActivity

object AppRatingsUIManager : MTLog.Loggable {

    private val LOG_TAG = AppRatingsUIManager::class.java.simpleName

    private const val TESTING_WITH_FAKE_REVIEW_MANAGER = false

    private const val USE_PLAY_API = true

    override fun getLogTag(): String = LOG_TAG

    @JvmStatic
    fun showAppRatingsUI(activity: IActivity, onAppRatingDisplayed: (Boolean) -> (Unit)) {
        if (Constants.DEBUG && BuildConfig.DEBUG) {
            showFakeUI(activity, onAppRatingDisplayed)
            return
        }
        if (USE_PLAY_API) {
            showPlayInAppReviewUI(activity, onAppRatingDisplayed)
        }
    }

    private fun showFakeUI(activity: IActivity, onAppRatingDisplayed: (Boolean) -> (Unit)) {
        val theContextView = activity.requireActivity().findViewById<View>(R.id.content_frame)
        val labelText = "App Rating UI test"
        val actionText = "Rate"
        val onActionClicked: (() -> Unit) = {
            onAppRatingDisplayed(true)
        }
        Snackbar.make(theContextView, labelText, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(actionText) {
                onActionClicked.invoke()
            }
        }.show()
    }

    // region Play In-App Review

    private fun makeReviewManager(context: Context) =
        if (TESTING_WITH_FAKE_REVIEW_MANAGER) FakeReviewManager(context) else
            ReviewManagerFactory.create(context)

    private fun showPlayInAppReviewUI(activity: IActivity, onAppRatingDisplayed: (Boolean) -> Unit) {
        val manager = makeReviewManager(activity.requireContext())
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity.requireActivity(), reviewInfo)
                flow.addOnCompleteListener { _ ->
                    onAppRatingDisplayed(true)
                }
            } else {
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                MTLog.w(this, task.exception, "Error while requesting review flow (code: $reviewErrorCode)")
            }
        }

    }

    // endregion

}