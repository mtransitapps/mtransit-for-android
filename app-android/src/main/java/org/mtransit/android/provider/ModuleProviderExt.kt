package org.mtransit.android.provider

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.data.toModule
import org.mtransit.android.provider.ModuleProvider.LOG_TAG

private val moduleJson = Json {
    allowTrailingComma = true // do not fail on trailing comma
    coerceInputValues = true // Falls back to default Kotlin values if org.json outputs null or mismatched types
    ignoreUnknownKeys = true // new keys might be added/updated between main app and agency module app releases
    isLenient = true // allows unquoted strings or malformed formatting (if legacy org.json writers were used)
}

fun String.toModules(authority: String) = jsonRunCatching(LOG_TAG) {
    moduleJson.parseToJsonElement(this).jsonArray.toModules(authority)
}.getOrThrow()

private fun JsonArray.toModules(authority: String) =
    this.mapIndexedNotNull { index, jsonElement ->
        jsonElement.jsonObject.toModule(
            authority = authority,
            id = index
        )
    }.toSet()
