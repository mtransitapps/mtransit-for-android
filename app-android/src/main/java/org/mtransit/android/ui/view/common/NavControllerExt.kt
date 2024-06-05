package org.mtransit.android.ui.view.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator

fun NavController.navigateF(
    @IdRes resId: Int,
    args: Bundle?,
    navOptions: NavOptions?,
    navigatorExtras: FragmentNavigator.Extras?
) {
    this.navigate(resId, args, navOptions, navigatorExtras as Navigator.Extras?)
}