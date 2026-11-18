package org.mtransit.android.ui.location

import android.annotation.SuppressLint
import org.mockito.kotlin.mock
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import android.location.Geocoder as AndroidGeocoder

class GeocoderManagerTest {

    @SuppressLint("DoNotMockPlatformTypes")
    private val geocoder: AndroidGeocoder = mock {}

    @Test
    fun test_getGeocoder_Locale() {
        var makeGeocoderCallCount = 0
        var lastLocale: Locale? = null
        var currentLocale = Locale.CANADA

        val subject = GeocoderManager(
            makeGeocoder = { locale ->
                lastLocale = locale
                makeGeocoderCallCount++
                geocoder
            },
            getCurrentLocale = { currentLocale },
        )

        subject.getGeocoder() // create 1st locale
        assertEquals(Locale.CANADA, lastLocale)
        assertEquals(1, makeGeocoderCallCount)

        subject.getGeocoder() // re-use
        assertEquals(1, makeGeocoderCallCount)

        currentLocale = Locale.CANADA_FRENCH
        subject.getGeocoder() // create 2nd locale
        assertEquals(Locale.CANADA_FRENCH, lastLocale)
        assertEquals(2, makeGeocoderCallCount)
    }
}
