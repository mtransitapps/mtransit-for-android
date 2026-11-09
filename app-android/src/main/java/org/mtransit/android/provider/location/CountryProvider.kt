package org.mtransit.android.provider.location

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountryProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val lclPrefRepository: LocalPreferenceRepository,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = CountryProvider::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    fun getCurrentCountryCode(): String? {
        MTLog.d(this, "getCurrentCountryCode()")
        getCurrentCountryCodeNetwork()?.let {
            MTLog.d(this, "getCurrentCountryCode() > use network country code: $it")
            return it
        }
        getCurrentCountryCodeFromIP()?.let {
            MTLog.d(this, "getCurrentCountryCode() > use IP country code: $it")
            return it
        }
        getCurrentCountryCodeFromLocale()?.let {
            MTLog.d(this, "getCurrentCountryCode() > use locale country code: $it")
            return it
        }
        MTLog.d(this, "getCurrentCountryCode() > not found")
        return null
    }

    private fun getCurrentCountryCodeFromLocale() =
        ConfigurationCompat.getLocales(appContext.resources.configuration).takeIf { !it.isEmpty }?.get(0)?.country
            ?.takeIf { it.isNotBlank() }

    private fun getCurrentCountryCodeNetwork() = try {
        appContext.getSystemService<TelephonyManager>()?.networkCountryIso?.uppercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
    } catch (_: UnsupportedOperationException) {
        null
    }

    private fun getCurrentCountryCodeFromIP() =
        lclPrefRepository.pref.getString(LocalPreferenceRepository.PREFS_LCL_IP_LOCATION_COUNTRY_CODE, null)
            ?.takeIf { it.isNotBlank() }
}
