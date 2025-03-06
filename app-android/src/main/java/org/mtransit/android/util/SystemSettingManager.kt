package org.mtransit.android.util

import android.content.Context
import android.provider.Settings

object SystemSettingManager {

    @JvmStatic
    fun isUsingFirebaseTestLab(context: Context) =
        Settings.System.getString(context.contentResolver, "firebase.test.lab") == "true"
}