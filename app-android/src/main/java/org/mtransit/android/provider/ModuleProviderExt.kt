package org.mtransit.android.provider

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.data.toModuleOrNull
import org.mtransit.android.provider.ModuleProvider.LOG_TAG
import org.mtransit.commons.jsonObjectOrNull

private val moduleJson = Json {
    allowTrailingComma = true // do not fail on trailing comma
    coerceInputValues = true // Falls back to default Kotlin values if org.json outputs null or mismatched types
    ignoreUnknownKeys = true // new keys might be added/updated between main app and agency module app releases
    isLenient = true // allows unquoted strings or malformed formatting (if legacy org.json writers were used)
}

fun String.toModulesOrThrow(authority: String) = jsonRunCatching(LOG_TAG) {
    moduleJson.parseToJsonElement(this).jsonArray.toModules(authority)
}.getOrThrow() // ignore (keep old data)

private fun JsonArray.toModules(authority: String) =
    this.mapIndexedNotNull { index, jsonElement ->
        jsonElement.jsonObjectOrNull?.toModuleOrNull(
            authority = authority,
            id = index
        )
    }.toSet()
