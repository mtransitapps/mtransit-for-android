package org.mtransit.android.provider.sensor

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import org.mtransit.android.ui.view.common.IFragment
import android.location.Location as AndroidLocation

interface MTSensorManager {

    fun registerCompassListener(sensorEventListener: SensorEventListener, listener: CompassListener)

    fun unregisterSensorListener(sensorEventListener: SensorEventListener)

    fun checkForCompass(
        activity: IFragment,
        event: SensorEvent,
        accelerometerValues: FloatArray,
        magneticFieldValues: FloatArray,
        listener: CompassListener
    )

    fun getLocationDeclination(location: AndroidLocation): Float

    fun updateCompass(
        force: Boolean,
        deviceLocation: AndroidLocation?,
        roundedOrientation: Int,
        now: Long,
        scrollState: Int,
        lastCompassChanged: Long,
        lastCompassInDegree: Int?,
        minThresholdInMs: Long,
        sensorTaskCompleted: SensorTaskCompleted
    )

    interface CompassListener {
        fun updateCompass(orientation: Float, force: Boolean)
    }

    interface SensorTaskCompleted {
        fun onSensorTaskCompleted(
            result: Boolean,
            orientation: Int,
            now: Long
        )
    }
}
