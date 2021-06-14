package org.mtransit.android.data

import java.util.Locale

interface IAgencyProperties {

    companion object {

        const val DEFAULT_VERSION_CODE = -1

        const val DEFAULT_LONG_VERSION_CODE = -1L

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
}