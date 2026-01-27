package org.mtransit.android.ui.view

import android.content.Context
import androidx.annotation.AnyThread
import com.google.android.gms.maps.MapsInitializer
import org.mtransit.android.commons.MTLog
import java.util.concurrent.atomic.AtomicReference

object MapsInitializerUtil : MTLog.Loggable {

    private val LOG_TAG: String = MapsInitializerUtil::class.java.simpleName

    private val rendererInitialized = AtomicReference<MapsInitializer.Renderer>(null)

    override fun getLogTag() = LOG_TAG

    @AnyThread
    @JvmStatic
    @JvmOverloads
    fun initMap(appContext: Context, callback: (MapsInitializer.Renderer?) -> (Unit) = { }) {
        rendererInitialized.get()?.let { renderer ->
            MTLog.d(this, "initMap() > SKIP (already running)")
            callback(renderer)
            return
        }
        try {
            MapsInitializer.initialize(appContext, MapsInitializer.Renderer.LATEST) { renderer ->
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> {
                        MTLog.d(this, "The latest version of the renderer is used.")
                    }
                    //noinspection deprecation
                    MapsInitializer.Renderer.LEGACY -> {
                        MTLog.d(this, "The legacy version of the renderer is used.")
                    }
                }
                this.rendererInitialized.set(renderer)
                callback(renderer)
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while initializing map!")
            this.rendererInitialized.set(null)
        }
    }
}
