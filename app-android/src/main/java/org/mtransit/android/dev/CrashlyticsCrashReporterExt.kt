package org.mtransit.android.dev

import com.google.firebase.crashlytics.FirebaseCrashlytics

val CrashlyticsCrashReporter.firebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }
