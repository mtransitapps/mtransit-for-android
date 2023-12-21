package org.mtransit.android.data

import android.content.Context
import android.content.pm.PackageManager
import org.mtransit.android.commons.isAppEnabled
import java.util.Locale

interface IAgencyProperties {

    companion object {

        const val DEFAULT_VERSION_CODE = -1

        const val DEFAULT_LONG_VERSION_CODE = -1L

        const val PKG_COMMON = "org.mtransit.android."

        @JvmStatic
        val SHORT_NAME_COMPARATOR: Comparator<IAgencyProperties> = Comparator { lap, rap ->
            lap.shortName.lowercase(Locale.getDefault())
                .compareTo(rap.shortName.lowercase(Locale.getDefault()))
        }

        fun removeType(agencies: MutableCollection<AgencyProperties>?, typeToRemove: DataSourceType) {
            agencies?.let {
                agencies.removeAll {
                    it.type == typeToRemove
                }
            }
        }
    }

    val authority: String

    val type: DataSourceType

    val pkg: String

    val isRTS: Boolean

    val shortName: String

    val isEnabled: Boolean

    fun getShortNameAndType(context: Context): String {
        return "$shortName ${context.getString(type.shortNameResId)}"
    }

    fun isEnabled(pm: PackageManager? = null): Boolean {
        return isEnabled && pm?.isAppEnabled(this.pkg) != false
    }
}