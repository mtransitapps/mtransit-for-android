package org.mtransit.android.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.data.applyTo
import org.mtransit.android.commons.data.getAuthorityFromJson
import org.mtransit.android.commons.data.getIdFromJson
import org.mtransit.android.commons.data.getLatFromJson
import org.mtransit.android.commons.data.getLngFromJson
import org.mtransit.android.commons.data.getNameFromJson
import org.mtransit.android.commons.data.toJsonCommon
import org.mtransit.android.commons.jsonRunCatching
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

fun Module.toJsonK() = jsonRunCatching {
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
}.getOrThrow()

fun JsonObject.toModuleOrNull(
    authority: String = getAuthorityFromJson(),
    id: Int = getIdFromJson(),
) = jsonRunCatching(LOG_TAG) {
    val optStore = this[JSON_STORE]?.jsonPrimitiveOrNull?.contentOrNull
    if (!BuildConfig.DEBUG && optStore != Module.STORE_PRODUCTION) return@jsonRunCatching null // ignore non-production agency modules in release builds
    Module(
        authority,
        id,
        getValue(JSON_PKG).jsonPrimitive.content,
        getValue(JSON_TYPE_ID).jsonPrimitive.int,
        getLatFromJson(),
        getLngFromJson(),
        getNameFromJson(),
    ).apply {
        color = this@toModuleOrNull[JSON_COLOR]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
        location = this@toModuleOrNull[JSON_LOCATION]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
        nameFr = this@toModuleOrNull[JSON_NAME_FR]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
        repo = this@toModuleOrNull[JSON_REPO]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }
        store = optStore?.takeIf { it.isNotEmpty() }
        applyTo(this)
    }
}.getOrNull()
