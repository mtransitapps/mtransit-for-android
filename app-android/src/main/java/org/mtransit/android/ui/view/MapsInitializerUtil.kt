package org.mtransit.android.ui.view

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.MapsInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

object MapsInitializerUtil : MTLog.Loggable {

    private val LOG_TAG: String = MapsInitializerUtil::class.java.simpleName

    private val PREFERRED_RENDERED = MapsInitializer.Renderer.LATEST

    private val rendererInitialized = AtomicReference<MapsInitializer.Renderer>(null)

    override fun getLogTag() = LOG_TAG

    suspend fun startInitMap(app: Application) = withContext(Dispatchers.IO) {
        initMap(app, onMapInitialized = {})
    }

    @MainThread
    @JvmStatic
    fun startInitMap(context: Context, @MainThread onMapInitialized: (MapsInitializer.Renderer?) -> (Unit)) {
        rendererInitialized.get()?.let { renderer ->
            MTLog.d(this, "initMap() > SKIP (already running)")
            onMapInitialized(renderer)
            return
        }
        val backgroundExecutor = Executors.newSingleThreadExecutor()
        backgroundExecutor.execute {
            initMap(context) { renderer ->
                ContextCompat.getMainExecutor(context).execute {
                    onMapInitialized(renderer)
                }
                backgroundExecutor.shutdown()
            }
        }
    }

    @WorkerThread
    private fun initMap(context: Context, onMapInitialized: (MapsInitializer.Renderer?) -> (Unit)) {
        rendererInitialized.get()?.let { renderer ->
            MTLog.d(this, "initMap() > SKIP (already running)")
            onMapInitialized(renderer)
            return
        }
        try {
            @Suppress("DEPRECATION")
            MapsInitializer.initialize(context, PREFERRED_RENDERED) { renderer ->
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
