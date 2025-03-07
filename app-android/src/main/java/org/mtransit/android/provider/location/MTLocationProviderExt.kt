package org.mtransit.android.provider.location

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun MTLocationProvider.getNearbyLocationAddress(location: Location?) = withContext(Dispatchers.IO) {
    location?.let { getLocationAddressString(it) }
}
