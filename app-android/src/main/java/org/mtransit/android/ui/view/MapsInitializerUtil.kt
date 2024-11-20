package org.mtransit.android.ui.view

import android.content.Context
import androidx.annotation.AnyThread
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import org.mtransit.android.commons.MTLog
import java.util.concurrent.atomic.AtomicBoolean

object MapsInitializerUtil : MTLog.Loggable {

    private val LOG_TAG = MapsInitializerUtil::class.java.simpleName

    private val RENDERER = MapsInitializer.Renderer.LATEST

    private val mapInitialized = AtomicBoolean(false)

    override fun getLogTag(): String = LOG_TAG

    @AnyThread
    @JvmStatic
    @JvmOverloads
    fun initMap(appContext: Context, callback: OnMapsSdkInitializedCallback? = null) {
        if (mapInitialized.getAndSet(true)) {
            MTLog.d(this, "initMap() > SKIP (already running)")
            callback?.onMapsSdkInitialized(RENDERER)
            return
        }
        try {
            // Initializes the Google Maps SDK for Android so that its classes are ready for use
            MapsInitializer.initialize(appContext, RENDERER) { renderer ->
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> {
                        MTLog.d(this, "The latest version of the renderer is used.")
                    }
                    //noinspection deprecation
                    MapsInitializer.Renderer.LEGACY -> {
                        MTLog.d(this, "The legacy version of the renderer is used.")
                    }
                }
                callback?.onMapsSdkInitialized(renderer)
                mapInitialized.set(true)
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while initializing map!")
            mapInitialized.set(false)
        }
    }
}
