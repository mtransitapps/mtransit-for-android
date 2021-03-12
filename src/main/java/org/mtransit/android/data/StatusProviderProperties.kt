package org.mtransit.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_provider_properties")
data class StatusProviderProperties(
    @PrimaryKey
    @ColumnInfo(name = "authority")
    val authority: String,
    @ColumnInfo(name = "target_authority")
    val targetAuthority: String
)