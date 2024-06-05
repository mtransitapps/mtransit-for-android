package org.mtransit.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_provider_properties",
)
data class ScheduleProviderProperties(
    @PrimaryKey
    @ColumnInfo(name = "authority")
    override val authority: String,
    @ColumnInfo(name = "target_authority")
    override val targetAuthority: String,
    @ColumnInfo(name = "pkg")
    override val pkg: String,
) : ITargetedProviderProperties