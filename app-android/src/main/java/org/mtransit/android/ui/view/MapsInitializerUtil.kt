package org.mtransit.android.ui.view

import android.content.Context
import androidx.annotation.MainThread
import com.google.android.gms.maps.MapsInitializer
import org.mtransit.android.commons.MTLog
import java.util.concurrent.atomic.AtomicReference

object MapsInitializerUtil : MTLog.Loggable {

    private val LOG_TAG: String = MapsInitializerUtil::class.java.simpleName

    private val PREFERRED_RENDERER = MapsInitializer.Renderer.LATEST

    private val rendererInitialized = AtomicReference<MapsInitializer.Renderer>(null)

    override fun getLogTag() = LOG_TAG

    /**
     * [MapsInitializer.initialize] has to be called from **Main Thread**.
     * There was wrong advice in on the Google Issue Tracker, but they since officially reversed that advice.
     * (do not call from [android.app.Application.onCreate] to avoid ANRs)
     * https://developers.google.com/android/reference/com/google/android/gms/maps/MapsInitializer
     */
    @JvmStatic
    @JvmOverloads
    @MainThread
    fun initMap(context: Context, onMapInitialized: (MapsInitializer.Renderer?) -> (Unit) = {}) {
        rendererInitialized.get()?.let { renderer ->
            MTLog.d(this, "initMap() > SKIP (already running)")
            onMapInitialized(renderer)
            return
        }
        try {
            @Suppress("DEPRECATION")
            MapsInitializer.initialize(context.applicationContext, PREFERRED_RENDERER) { renderer ->
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> {
                        MTLog.d(this, "The latest version of the renderer is used.")
                    }

                    MapsInitializer.Renderer.LEGACY -> {
                        MTLog.d(this, "The legacy version of the renderer is used.")
                    }
                }
                rendererInitialized.set(renderer)
                onMapInitialized(renderer)
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while initializing map!")
            rendererInitialized.set(null)
        }
    }
}
