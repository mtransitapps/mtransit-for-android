package org.mtransit.android.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.applyTo
import org.mtransit.android.commons.data.getAuthorityFromJson
import org.mtransit.android.commons.data.getIdFromJson
import org.mtransit.android.commons.data.getLatFromJson
import org.mtransit.android.commons.data.getLngFromJson
import org.mtransit.android.commons.data.getNameFromJson
import org.mtransit.android.commons.data.toJsonCommon
import org.mtransit.android.data.Module.LOG_TAG
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_PKG = "pkg"
private const val JSON_TYPE_ID = "type_id"
private const val JSON_COLOR = "color"
private const val JSON_LOCATION = "location"
private const val JSON_NAME_FR = "name_fr"
private const val JSON_REPO = "repo"
private const val JSON_STORE = "store"

fun Module.toJsonK() = try {
    buildJsonObject {
        putK(JSON_PKG, this@toJsonK.pkg)
        putK(JSON_TYPE_ID, this@toJsonK.dstId)
        this@toJsonK.color?.takeIf { it.isNotEmpty() }?.let { putK(JSON_COLOR, it) }
        this@toJsonK.location?.takeIf { it.isNotEmpty() }?.let { putK(JSON_LOCATION, it) }
        this@toJsonK.nameFr?.takeIf { it.isNotEmpty() }?.let { putK(JSON_NAME_FR, it) }
        this@toJsonK.repo?.takeIf { it.isNotEmpty() }?.let { putK(JSON_REPO, it) }
        this@toJsonK.store?.takeIf { it.isNotEmpty() }?.let { putK(JSON_STORE, it) }
        toJsonCommon(this@toJsonK)
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", iae)
    throw iae
}

fun JsonObject.toModule(
    authority: String = getAuthorityFromJson(),
    id: Int = getIdFromJson(),
): Module? {
    try {
        val optStore = this[JSON_STORE]?.jsonPrimitiveOrNull?.contentOrNull
        if (!BuildConfig.DEBUG && Module.STORE_PRODUCTION != optStore) return null // ignore non-production agency modules in release builds
        return Module(
            authority,
            id,
            getValue(JSON_PKG).jsonPrimitive.content,
            getValue(JSON_TYPE_ID).jsonPrimitive.int,
            getLatFromJson(),
            getLngFromJson(),
            getNameFromJson(),
        ).apply {
            color = this@toModule[JSON_COLOR]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
            location = this@toModule[JSON_LOCATION]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
            nameFr = this@toModule[JSON_NAME_FR]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
            repo = this@toModule[JSON_REPO]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
            store = optStore?.takeIf { it.isNotEmpty() }
            applyTo(this)
        }
    } catch (iae: IllegalArgumentException) {
        MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
        throw iae
    }
}
