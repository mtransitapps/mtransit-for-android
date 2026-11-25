package org.mtransit.android.datasource

import androidx.room.TypeConverter
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.toJPaths
import org.mtransit.android.data.toJsonString

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
    fun fromJsonString(jsonString: String?): JPaths? {
        return jsonString?.toJPaths()
    }

    @TypeConverter
    fun jPathsToJsonString(jPaths: JPaths?): String? {
        return jPaths?.toJsonString()
    }
}
