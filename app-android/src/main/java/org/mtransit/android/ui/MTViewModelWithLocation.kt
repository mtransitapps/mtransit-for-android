package org.mtransit.android.ui

import android.app.PendingIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.location.toStringSimple
import android.location.Location as AndroidLocation

abstract class MTViewModelWithLocation : ViewModel(), MTLog.Loggable {

    private val _locationSettingsResolution = MutableLiveData<PendingIntent?>()

    val locationSettingsResolution: LiveData<PendingIntent?> = _locationSettingsResolution

    fun onLocationSettingsResolution(newResolution: PendingIntent?) {
        _locationSettingsResolution.value = newResolution
    }

    private val _deviceLocation = MutableLiveData<AndroidLocation?>()

    val deviceLocation: LiveData<AndroidLocation?> = _deviceLocation

    fun onDeviceLocationChanged(newDeviceLocation: AndroidLocation?, force: Boolean = false) {
        if (force) {
            MTLog.d(this, "onDeviceLocationChanged() > save new forced location '${newDeviceLocation?.toStringSimple()}'.")
            _deviceLocation.value = newDeviceLocation
            return
        }
        newDeviceLocation?.let {
            val currentDeviceLocation = _deviceLocation.value
            if (currentDeviceLocation == null || LocationUtils.isMoreRelevant(logTag, currentDeviceLocation, it)) {
                MTLog.d(this, "onDeviceLocationChanged() > save new more relevant location '${newDeviceLocation.toStringSimple()}'.")
                _deviceLocation.value = it
            }
        }
    }
}
