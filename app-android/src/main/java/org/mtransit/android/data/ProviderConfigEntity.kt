package org.mtransit.android.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_config")
data class ProviderConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "authority")
    val authority: String,
    @ColumnInfo(name = "pkg")
    val pkg: String,
    @ColumnInfo(name = "module_version_code")
    val moduleVersionCode: Long,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    @ColumnInfo(name = "config_json")
    val configJson: String,
) {
    override fun toString() = "ProviderConfigEntity(authority=$authority, pkg=$pkg, moduleVersionCode=$moduleVersionCode)"
}
