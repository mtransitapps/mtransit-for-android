package org.mtransit.android.ui.view.common

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.transition.Transition
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import org.mtransit.android.R
import org.mtransit.android.commons.Constants
import org.mtransit.commons.FeatureFlags

/**
 * Android Dev:
 * https://developer.android.com/guide/fragments/animate#shared
 * https://developer.android.com/guide/navigation/navigation-animate-transitions#set-fragment
 * https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
 * https://medium.com/androiddevelopers/fragment-transitions-ea2726c3f36f
 * Material:
 * https://material.io/develop/android/theming/motion#container-transform
 * https://material.io/develop/android/theming/motion#customization
 * Blog:
 * https://wykorijnsburger.nl/dev/2019/04/30/shared-element-transition/
 *
 * TODO:
 * - look at visibility changing during transition (enter + exit)
 *  - POI scree
 *  - Home screen
 */
object MTTransitions {

    const val DURATION_FACTOR = 1L
    const val OVERRIDE_SCRIM_COLOR = false

    @JvmStatic
    fun setTransitionName(view: View?, transitionName: String?) {
        if (!FeatureFlags.F_TRANSITION) {
            return
        }
        view?.transitionName = transitionName
    }

    @JvmStatic
    fun newHoldTransition(): Transition? {
        if (!FeatureFlags.F_TRANSITION) {
            return null
        }
        return Hold()
    }

    @JvmStatic
    fun setContainerTransformTransition(fragment: Fragment) {
        if (!FeatureFlags.F_TRANSITION) {
            return
        }
        fragment.enterTransition = newContainerTransformTransition(fragment.context)
    }

    @JvmOverloads
    @JvmStatic
    fun newContainerTransformTransition(
        context: Context?,
        transitionScrimColor: Int? = context?.let { ContextCompat.getColor(it, R.color.color_background) },
    ): Transition? {
        if (!FeatureFlags.F_TRANSITION) {
            return null
        }
        return MaterialContainerTransform().apply {
            duration *= DURATION_FACTOR
            if (OVERRIDE_SCRIM_COLOR) {
                transitionScrimColor?.let {
                    scrimColor = transitionScrimColor
                }
            }
            if (Constants.DEBUG_TRANSITION) {
                isDrawDebugEnabled = true
                // TODO ? drawingViewId = R.id.scrollview
                // TODO ? pathMotion = ArcMotion()
                duration *= DURATION_FACTOR // double factor
                if (OVERRIDE_SCRIM_COLOR) {
                    scrimColor = Color.RED
                }
            }
        }
    }

    @JvmStatic
    fun postponeEnterTransition(fragment: Fragment) {
        if (!FeatureFlags.F_TRANSITION) {
            return
        }
        fragment.postponeEnterTransition()
    }

    @JvmStatic
    fun startPostponedEnterTransitionOnPreDraw(view: View?, fragment: Fragment) {
        if (!FeatureFlags.F_TRANSITION) {
            return
        }
        (view as? ViewGroup)?.doOnPreDraw {
            fragment.startPostponedEnterTransition()
        }
    }
}