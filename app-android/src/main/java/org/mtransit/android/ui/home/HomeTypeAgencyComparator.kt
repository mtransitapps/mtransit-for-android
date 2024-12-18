package org.mtransit.android.ui.home

import android.content.Context
import android.location.Location
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import java.lang.ref.WeakReference

class HomeTypeAgencyComparator(
    private val contextWR: WeakReference<Context>,
    private val typeToAgencies: Map<DataSourceType, List<AgencyBaseProperties>>,
    private val location: Location,
) : Comparator<DataSourceType> {

    constructor(
        context: Context,
        typeToAgencies: Map<DataSourceType, List<AgencyBaseProperties>>,
        location: Location,
    ) : this(WeakReference(context), typeToAgencies, location)

    private val typeToInsideArea: Map<DataSourceType, Boolean> by lazy {
        typeToAgencies.map { (type, agencies) ->
            type to agencies.any { agency -> agency.isLocationInside(location) }
        }.toMap()
    }

    override fun compare(lType: DataSourceType, rType: DataSourceType): Int {
        if (DataSourceType.TYPE_MODULE == lType) {
            return ComparatorUtils.AFTER
        } else if (DataSourceType.TYPE_MODULE == rType) {
            return ComparatorUtils.BEFORE
        } else if (DataSourceType.TYPE_PLACE == lType) {
            return ComparatorUtils.AFTER
        } else if (DataSourceType.TYPE_PLACE == rType) {
            return ComparatorUtils.BEFORE
        }
        if (typeToInsideArea[lType] != typeToInsideArea[rType]) {
            return if (typeToInsideArea[lType] == true) {
                ComparatorUtils.BEFORE
            } else {
                ComparatorUtils.AFTER
            }
        }
        if (lType == rType) {
            return ComparatorUtils.SAME
        }
        val context = contextWR.get()
            ?: return ComparatorUtils.SAME
        val lShortName = context.getString(lType.shortNameResId)
        val rShortName = context.getString(rType.shortNameResId)
        return lShortName.compareTo(rShortName)
    }
}