package org.mtransit.android.provider.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.Surface
import android.widget.AbsListView
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.api.SupportFactory
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs


class SensorManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : MTSensorManager, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = SensorManagerImpl::class.java.simpleName

        private const val COMPASS_DEGREE_UPDATE_THRESHOLD = 10 // 10°

        private val COMPASS_UPDATE_THRESHOLD_IN_MS = TimeUnit.MILLISECONDS.toMillis(250L)
    }

    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

    override fun getLogTag() = LOG_TAG

    override fun registerCompassListener(sensorEventListener: SensorEventListener) {
        sensorManager?.let {
            it.registerListener(
                sensorEventListener,
                getAccelerometerSensor(sensorManager),
                SensorManager.SENSOR_DELAY_UI
            )
            it.registerListener(
                sensorEventListener,
                getMagneticFieldSensor(sensorManager),
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun getAccelerometerSensor(sensorManager: SensorManager): Sensor? {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun getMagneticFieldSensor(mSensorManager: SensorManager): Sensor? {
        return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun unregisterSensorListener(sensorEventListener: SensorEventListener) {
        sensorManager?.unregisterListener(sensorEventListener)
    }

    override fun checkForCompass(
        activity: IActivity,
        event: SensorEvent,
        accelerometerValues: FloatArray,
        magneticFieldValues: FloatArray,
        listener: MTSensorManager.CompassListener
    ) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(
                    event.values,
                    0,
                    accelerometerValues,
                    0,
                    event.values.size
                )
                if (magneticFieldValues.size == 3
                    && magneticFieldValues[0] != 0.0f
                    && magneticFieldValues[1] != 0.0f
                    && magneticFieldValues[2] != 0.0f
                ) {
                    calculateOrientation(
                        activity,
                        accelerometerValues,
                        magneticFieldValues
                    )?.let {
                        listener.updateCompass(it, false)
                    }
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(
                    event.values,
                    0,
                    magneticFieldValues,
                    0,
                    event.values.size
                )
                if (accelerometerValues.size == 3
                    && accelerometerValues[0] != 0.0f
                    && accelerometerValues[1] != 0.0f
                    && accelerometerValues[2] != 0.0f
                ) {
                    calculateOrientation(
                        activity,
                        accelerometerValues,
                        magneticFieldValues
                    )?.let {
                        listener.updateCompass(it, false)
                    }
                }
            }
            else -> {
                // DO NOTING
            }
        }
    }

    private fun calculateOrientation(
        activity: IActivity,
        accelerometerValues: FloatArray?,
        magneticFieldValues: FloatArray?
    ): Float? {
        if (accelerometerValues == null
            || accelerometerValues.size != 3
            || magneticFieldValues == null
            || magneticFieldValues.size != 3
        ) {
            return null
        }
        val rArray = FloatArray(9)
        val success =
            SensorManager.getRotationMatrix(rArray, null, accelerometerValues, magneticFieldValues)
        if (!success) {
            return null
        }
        val axis = IntArray(2)
        axis[0] = SensorManager.AXIS_X
        axis[1] = SensorManager.AXIS_Y
        val aActivity = activity.activity ?: return null
        val defaultDisplay = SupportFactory.get().getDefaultDisplay(aActivity)
        when (defaultDisplay?.rotation) {
            Surface.ROTATION_0 -> {
                // DO NOTHING
            }
            Surface.ROTATION_90 -> {
                axis[0] = SensorManager.AXIS_Y
                axis[1] = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                axis[1] = SensorManager.AXIS_MINUS_Y
            }
            Surface.ROTATION_270 -> {
                axis[0] = SensorManager.AXIS_MINUS_Y
                axis[1] = SensorManager.AXIS_X
            }
            else -> {
                // DO NOTHING
            }
        }
        val outR = FloatArray(9)
        if (!SensorManager.remapCoordinateSystem(rArray, axis[0], axis[1], outR)) {
            return null
        }
        val values = FloatArray(3)
        SensorManager.getOrientation(outR, values)
        values[0] = Math.toDegrees(values[0].toDouble()).toFloat()
        values[1] = Math.toDegrees(values[1].toDouble()).toFloat()
        values[2] = Math.toDegrees(values[2].toDouble()).toFloat()
        return values[0]
    }

    override fun getLocationDeclination(location: Location): Float {
        return GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            location.time
        ).declination
    }

    override fun updateCompass(
        force: Boolean,
        userLocation: Location?,
        roundedOrientation: Int,
        now: Long,
        scrollState: Int,
        lastCompassChanged: Long,
        lastCompassInDegree: Int,
        minThresholdInMs: Long,
        sensorTaskCompleted: MTSensorManager.SensorTaskCompleted
    ) {
        if (userLocation == null || roundedOrientation < 0) {
            sensorTaskCompleted.onSensorTaskCompleted(false, roundedOrientation, now)
            return
        }
        if (!force) {
            if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                sensorTaskCompleted.onSensorTaskCompleted(false, roundedOrientation, now)
                return
            }
            val diffInMs = now - lastCompassChanged
            val tooSoon = diffInMs <= minThresholdInMs.coerceAtLeast(COMPASS_UPDATE_THRESHOLD_IN_MS)
            if (tooSoon) {
                sensorTaskCompleted.onSensorTaskCompleted(false, roundedOrientation, now)
                return
            }
            val diffInDegree: Float = abs(lastCompassInDegree - roundedOrientation).toFloat()
            val notDifferentEnough = diffInDegree <= COMPASS_DEGREE_UPDATE_THRESHOLD
            if (notDifferentEnough) {
                sensorTaskCompleted.onSensorTaskCompleted(false, roundedOrientation, now)
                return
            }
        }
        sensorTaskCompleted.onSensorTaskCompleted(true, roundedOrientation, now)
    }
}