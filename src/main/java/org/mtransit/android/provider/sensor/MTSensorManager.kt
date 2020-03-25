package org.mtransit.android.provider.sensor

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Location

interface MTSensorManager {

    fun registerCompassListener(sensorEventListener: SensorEventListener)

    fun unregisterSensorListener(sensorEventListener: SensorEventListener)

    fun checkForCompass(
        event: SensorEvent,
        accelerometerValues: FloatArray,
        magneticFieldValues: FloatArray,
        listener: CompassListener
    )

    fun getLocationDeclination(location: Location): Float

    fun updateCompass(
        force: Boolean,
        userLocation: Location?,
        roundedOrientation: Int,
        now: Long,
        scrollState: Int,
        lastCompassChanged: Long,
        lastCompassInDegree: Int,
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