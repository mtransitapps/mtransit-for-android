package org.mtransit.android.device

import android.content.Context
import androidx.annotation.IntRange
import androidx.core.performance.DevicePerformance
import androidx.core.performance.play.services.PlayServicesDevicePerformance
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext appContext: Context,
) {

    private val devicePerformance: DevicePerformance by lazy { PlayServicesDevicePerformance(appContext) }

    private val mediaPerformanceClass: Int by lazy { devicePerformance.mediaPerformanceClass }

    fun isAtLeast(@IntRange(from = 30) minPerformanceClass: Int) = mediaPerformanceClass >= minPerformanceClass
}
