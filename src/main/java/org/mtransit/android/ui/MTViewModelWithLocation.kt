package org.mtransit.android.ui

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog

abstract class MTViewModelWithLocation : ViewModel(), MTLog.Loggable {

    private val _deviceLocation = MutableLiveData<Location?>()

    val deviceLocation: LiveData<Location?> = _deviceLocation

    fun onDeviceLocationChanged(newDeviceLocation: Location?) {
        newDeviceLocation?.let {
            val currentDeviceLocation = _deviceLocation.value
            if (currentDeviceLocation == null || LocationUtils.isMoreRelevant(logTag, currentDeviceLocation, it)) {
                _deviceLocation.value = it
            }
        }
    }
}