package org.mtransit.android.ui.view.map

import androidx.collection.ArrayMap
import androidx.collection.SimpleArrayMap
import org.mtransit.android.commons.MTLog

data class MTPOIMarkerIds(
    private val uuidsAndAuthority: ArrayMap<String, String> = ArrayMap()
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = MTPOIMarkerIds::class.java.simpleName

        @JvmStatic
        fun from(uuid: String, authority: String?) =
            MTPOIMarkerIds().apply {
                put(uuid, authority)
            }
    }

    override fun getLogTag() = LOG_TAG

    fun entrySet(): Set<Map.Entry<String?, String?>> = this.uuidsAndAuthority.entries

    fun hasUUID(uuid: String?): Boolean = this.uuidsAndAuthority.containsKey(uuid)

    val map: ArrayMap<String, String> get() = this.uuidsAndAuthority

    fun put(uuid: String, authority: String?) {
        try {
            this.uuidsAndAuthority[uuid] = authority
        } catch (e: Exception) {
            @Suppress("DEPRECATION") // FIXME
            org.mtransit.android.util.CrashUtils.w(this, e, "Error while adding POI marker ID %s:%s", uuid, authority)
        }
    }

    fun putAll(newUuidsAndAuthority: SimpleArrayMap<String?, String?>?) {
        newUuidsAndAuthority?.let { this.uuidsAndAuthority.putAll(it) }
    }

    fun merge(poiMarkerIds: MTPOIMarkerIds?) {
        poiMarkerIds?.let { putAll(it.uuidsAndAuthority) }
    }

    fun size() = this.uuidsAndAuthority.size
}
