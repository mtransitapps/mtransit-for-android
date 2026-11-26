@file:Suppress("TooManyFunctions")

package org.mtransit.android.data

import android.graphics.Paint
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.data.JPaths.JCircle
import org.mtransit.android.data.JPaths.JForm
import org.mtransit.android.data.JPaths.JPaint
import org.mtransit.android.data.JPaths.JPath
import org.mtransit.android.data.JPaths.JRect
import org.mtransit.android.data.JPaths.JRotation
import org.mtransit.android.data.JPaths.LOG_TAG
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private val pathsJson = Json {
    allowTrailingComma = true // do not fail on trailing comma
    coerceInputValues = true // Falls back to default Kotlin values if org.json outputs null or mismatched types
    ignoreUnknownKeys = true // new keys might be added/updated between main app and agency module app releases
    isLenient = true // allows unquoted strings or malformed formatting (if legacy org.json writers were used)
}

// region JPaths

private const val JSON_ID = "id"
private const val JSON_PATHS = "paths"

fun JPaths.toJsonStringOrNull() = jsonRunCatching { pathsJson.encodeToString(toJson()) }.getOrNull()

@VisibleForTesting
internal fun JPaths.toJson() = buildJsonObject {
    putK(JSON_ID, this@toJson.id)
    putJsonArray(JSON_PATHS) {
        addAll(this@toJson.paths.mapNotNull(JPath::toJson))
    }
}

fun String?.toJPathsOrNull() = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    pathsJson.parseToJsonElement(this).jsonObjectOrNull?.toJPaths()
}?.getOrNull()

@VisibleForTesting
internal fun JsonObject.toJPaths() = JPaths(
    getValue(JSON_ID).jsonPrimitive.content
).apply {
    paths.addAll(
        get(JSON_PATHS)?.jsonArrayOrNull?.mapNotNull {
            it.jsonObjectOrNull?.toJPathOrNull()
        }.orEmpty()
    )
}

// endregion

// region JPath

private const val JSON_PAINT = "paint"
private const val JSON_FORM = "form"
private const val JSON_ROTATION = "rotation"

private fun JPath.toJson() = jsonRunCatching(LOG_TAG) {
    buildJsonObject {
        paint.toJsonOrNull()?.let { put(JSON_PAINT, it) }
        form.toJsonOrNull()?.let { put(JSON_FORM, it) }
        rotation?.toJsonOrNull()?.let { put(JSON_ROTATION, it) }
    }
}.getOrNull()

private fun JsonObject.toJPathOrNull() = jsonRunCatching(LOG_TAG) {
    JPath(
        getValue(JSON_PAINT).jsonObject.toJPaint(),
        getValue(JSON_FORM).jsonObject.toJForm(),
        this[JSON_ROTATION]?.jsonObjectOrNull?.toJRotationOrNull(),
    )
}.getOrNull()

// endregion

// region JPaint

private const val JSON_STYLE = "style"
private const val JSON_STROKE_WIDTH = "strokeWidth"

private fun JPaint.toJsonOrNull() = jsonRunCatching(LOG_TAG) {
    buildJsonObject {
        putK(JSON_STYLE, style.name)
        if (strokeWidth >= 0f) {
            putK(JSON_STROKE_WIDTH, strokeWidth)
        }
    }
}.getOrNull()

private fun JsonObject.toJPaint() = jsonRunCatching(LOG_TAG) {
    val style = Paint.Style.valueOf(getValue(JSON_STYLE).jsonPrimitive.content)
    val strokeWidth = this[JSON_STROKE_WIDTH]?.jsonPrimitiveOrNull?.contentOrNull?.toFloatOrNull() ?: -1f
    if (strokeWidth >= 0f) JPaint(style, strokeWidth) else JPaint(style)
}.getOrThrow()

// endregion

// region JForm

private const val JSON_FORM_TYPE = "formType"

private fun JForm.toJsonOrNull() = jsonRunCatching(LOG_TAG) {
    when (this) {
        is JCircle -> toJson()
        is JRect -> toJson()
        else -> throw IllegalArgumentException("Unsupported form type: ${this::class.simpleName}")
    }
}.getOrNull()

private fun JsonObject.toJForm() = jsonRunCatching(LOG_TAG) {
    when (getValue(JSON_FORM_TYPE).jsonPrimitive.int) {
        JForm.FORM_CIRCLE -> toJCircle()
        JForm.FORM_RECT -> toJRect()
        else -> throw IllegalArgumentException("Unsupported form type ID: $this")
    }
}.getOrThrow()

// region JCircle

private const val JSON_X = "x"
private const val JSON_Y = "y"
private const val JSON_RADIUS = "radius"

private fun JCircle.toJson() = buildJsonObject {
    putK(JSON_X, x)
    putK(JSON_Y, y)
    putK(JSON_RADIUS, radius)
    putK(JSON_FORM_TYPE, formType)
}

private fun JsonObject.toJCircle() = jsonRunCatching(LOG_TAG) {
    JCircle(
        getValue(JSON_X).jsonPrimitive.float,
        getValue(JSON_Y).jsonPrimitive.float,
        getValue(JSON_RADIUS).jsonPrimitive.float,
    )
}.getOrThrow()

// endregion

// region JRect

private const val JSON_LEFT = "left"
private const val JSON_TOP = "top"
private const val JSON_RIGHT = "right"
private const val JSON_BOTTOM = "bottom"

private fun JRect.toJson() = buildJsonObject {
    putK(JSON_LEFT, left)
    putK(JSON_TOP, top)
    putK(JSON_RIGHT, right)
    putK(JSON_BOTTOM, bottom)
    putK(JSON_FORM_TYPE, formType)
}

private fun JsonObject.toJRect() = jsonRunCatching(LOG_TAG) {
    JRect(
        getValue(JSON_LEFT).jsonPrimitive.float,
        getValue(JSON_TOP).jsonPrimitive.float,
        getValue(JSON_RIGHT).jsonPrimitive.float,
        getValue(JSON_BOTTOM).jsonPrimitive.float,
    )
}.getOrThrow()

// endregion

// endregion

// region JRotation

private const val JSON_DEGREES = "degrees"
private const val JSON_PX = "px"
private const val JSON_PY = "py"

private fun JRotation.toJsonOrNull() = jsonRunCatching(LOG_TAG) {
    buildJsonObject {
        putK(JSON_DEGREES, degrees)
        putK(JSON_PX, px)
        putK(JSON_PY, py)
    }
}.getOrNull()

private fun JsonObject.toJRotationOrNull() = jsonRunCatching(LOG_TAG) {
    JRotation(
        getValue(JSON_DEGREES).jsonPrimitive.float,
        getValue(JSON_PX).jsonPrimitive.float,
        getValue(JSON_PY).jsonPrimitive.float,
    )
}.getOrNull()

// endregion
