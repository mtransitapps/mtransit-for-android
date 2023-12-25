package org.mtransit.android.provider.location.network

import android.content.Context
import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.pref.liveDataN
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkLocationRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val apiService: IPWhoIsApiService,
    private val lclPreferenceRepository: LocalPreferenceRepository,
    private val dataSourcesRepository: DataSourcesRepository,
    private val locationPermissionProvider: LocationPermissionProvider,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = NetworkLocationRepository::class.java.simpleName

        private val DEFAULT_LOCATION_VALUE: Float? = null

        private val MIN_INTERVAL_BETWEEN_IP_LOCATION_CHECK_MS = TimeUnit.HOURS.toMillis(1L)

        private const val NETWORK_LOCATION_ACCURACY_IN_METERS: Float = 25_000.0F
    }

    override fun getLogTag(): String = LOG_TAG

    private var _ipLocationLat = lclPreferenceRepository.pref.liveDataN(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_LAT, DEFAULT_LOCATION_VALUE)
    private var _ipLocationLng = lclPreferenceRepository.pref.liveDataN(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_LNG, DEFAULT_LOCATION_VALUE)

    private val _agenciesCount = dataSourcesRepository.readingAllAgenciesCount()

    val ipLocation: LiveData<Location?> = TripleMediatorLiveData(_ipLocationLat, _ipLocationLng, _agenciesCount).switchMap { (lat, lng, agenciesCount) ->
        liveData {
            if (lat != null && lng != null && agenciesCount != null && agenciesCount <= DataSourcesRepository.DEFAULT_AGENCY_COUNT) {
                emit(LocationUtils.getNewLocation(lat.toDouble(), lng.toDouble(), NETWORK_LOCATION_ACCURACY_IN_METERS))
            }
        }
    }

    suspend fun fetchIPLocationIfNecessary(deviceLocation: Location?) {
        if (deviceLocation != null) {
            return
        }
        if (locationPermissionProvider.allRequiredPermissionsGranted(appContext)) {
            return
        }
        if (dataSourcesRepository.getAllAgenciesCount() > DataSourcesRepository.DEFAULT_AGENCY_COUNT) {
            return
        }
        val lastCheckInMs = lclPreferenceRepository.getValue(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_TIMESTAMP, -1L)
        if (lastCheckInMs + MIN_INTERVAL_BETWEEN_IP_LOCATION_CHECK_MS > TimeUtils.currentTimeMillis()) {
            return // SKIP too soon
        }
        runCatching {
            apiService.getCurrentIPLocation()
        }.fold(
            onSuccess = { (lat, lng) ->
                if (lat != null && lng != null) {
                    lclPreferenceRepository.pref.edit {
                        putFloat(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_LAT, lat)
                        putFloat(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_LNG, lng)
                        putLong(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_TIMESTAMP, TimeUtils.currentTimeMillis())

                    }
                }
            },
            onFailure = { t ->
                MTLog.e(this@NetworkLocationRepository, t, "Error while fetching current IP location!")
            }
        )
    }
}