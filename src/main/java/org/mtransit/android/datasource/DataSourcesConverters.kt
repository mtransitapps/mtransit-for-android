package org.mtransit.android.datasource

import androidx.room.TypeConverter
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.JPaths

class DataSourcesConverters {

    @TypeConverter
    fun fromTypeId(typeId: Int?): DataSourceType? {
        return typeId?.let { DataSourceType.parseId(it) }
    }

    @TypeConverter
    fun dataSourceTypeToTypeId(dst: DataSourceType?): Int? {
        return dst?.id
    }

    @TypeConverter
    fun fromJSONString(jsonString: String?): JPaths? {
        return jsonString?.let { JPaths.fromJSONString(it) }
    }

    @TypeConverter
    fun jPathsToJSONString(jPaths: JPaths?): String? {
        return jPaths?.let { it.toJSON()?.toString() }
    }
}