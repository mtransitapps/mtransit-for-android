package org.mtransit.android.provider

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.data.toModule
import org.mtransit.android.provider.ModuleProvider.LOG_TAG

fun String.toModules(authority: String) = try {
    sharedJson.parseToJsonElement(this).jsonArray.toModules(authority)
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", se)
    throw se
}

private fun JsonArray.toModules(authority: String) =
    this.mapIndexedNotNull { index, jsonElement ->
        jsonElement.jsonObject.toModule(
            authority = authority,
            id = index
        )
    }.toSet()
