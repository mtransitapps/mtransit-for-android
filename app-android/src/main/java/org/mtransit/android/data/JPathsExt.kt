package org.mtransit.android.data

import android.graphics.Paint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.data.JPaths.JCircle
import org.mtransit.android.data.JPaths.JForm
import org.mtransit.android.data.JPaths.JPaint
import org.mtransit.android.data.JPaths.JPath
import org.mtransit.android.data.JPaths.JRect
import org.mtransit.android.data.JPaths.JRotation
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

fun JPaths.toJsonString() = pathsJson.encodeToString(toJsonK())

fun JPaths.toJsonK() = buildJsonObject {
    putK(JSON_ID, this@toJsonK.id)
    putJsonArray(JSON_PATHS) {
        addAll(this@toJsonK.paths.mapNotNull(JPath::toJsonK))
    }
}

fun String?.toJPaths() = try {
    this?.let { pathsJson.parseToJsonElement(it).jsonObjectOrNull?.toJPaths() }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing JSON string '$this'!", iae)
    throw iae
}

fun JsonObject.toJPaths() = try {
    JPaths(getValue(JSON_ID).jsonPrimitive.content)
        .apply {
            this.paths.addAll(
                this@toJPaths[JSON_PATHS]?.jsonArrayOrNull?.mapNotNull {
                    it.jsonObjectOrNull?.toJPath()
                }.orEmpty()
            )
        }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

// endregion

// region JPath

private const val JSON_PAINT = "paint"
private const val JSON_FORM = "form"
private const val JSON_ROTATION = "rotation"

private fun JPath.toJsonK() = try {
    buildJsonObject {
        put(JSON_PAINT, paint.toJsonK())
        put(JSON_FORM, form.toJsonK())
        rotation?.let { put(JSON_ROTATION, it.toJsonK()) }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while converting to JSON!", iae)
    null
}

private fun JsonObject.toJPath(): JPath? {
    try {
        return JPath(
            getValue(JSON_PAINT).jsonObject.toJPaint() ?: return null,
            getValue(JSON_FORM).jsonObject.toJForm() ?: return null,
            this[JSON_ROTATION]?.jsonObjectOrNull?.toJRotation(),
        )
    } catch (iae: IllegalArgumentException) {
        MTLogK.w(JPaths.LOG_TAG, "Error while parsing path JSON '$this'!", iae)
        throw iae
    }
}

// endregion

// region JPaint

private const val JSON_STYLE = "style"
private const val JSON_STROKE_WIDTH = "strokeWidth"

fun JPaint.toJsonK() = buildJsonObject {
    putK(JSON_STYLE, style.name)
    if (strokeWidth >= 0f) {
        putK(JSON_STROKE_WIDTH, strokeWidth)
    }
}

fun JsonObject.toJPaint() = try {
    val style = Paint.Style.valueOf(getValue(JSON_STYLE).jsonPrimitive.content)
    val strokeWidth = this[JSON_STROKE_WIDTH]?.jsonPrimitiveOrNull?.contentOrNull?.toFloatOrNull() ?: -1f
    if (strokeWidth >= 0f) JPaint(style, strokeWidth) else JPaint(style)
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing paint JSON '$this'!", iae)
    null
}

// endregion

// region JForm

private const val JSON_FORM_TYPE = "formType"

fun JForm.toJsonK(): JsonObject = when (this) {
    is JCircle -> toJsonK()
    is JRect -> toJsonK()
    else -> throw IllegalArgumentException("Unsupported form type: ${this::class.simpleName}")
}

fun JsonObject.toJForm(): JForm? = try {
    when (getValue(JSON_FORM_TYPE).jsonPrimitive.int) {
        JForm.FORM_CIRCLE -> toJCircle()
        JForm.FORM_RECT -> toJRect()
        else -> {
            MTLogK.w(JPaths.LOG_TAG, "Unexpected form type ID in '$this'!")
            null
        }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing form JSON '$this'!", iae)
    throw iae
}

// region JCircle

private const val JSON_X = "x"
private const val JSON_Y = "y"
private const val JSON_RADIUS = "radius"

fun JCircle.toJsonK() = buildJsonObject {
    putK(JSON_X, x)
    putK(JSON_Y, y)
    putK(JSON_RADIUS, radius)
    putK(JSON_FORM_TYPE, formType)
}

fun JsonObject.toJCircle() = try {
    JCircle(
        getValue(JSON_X).jsonPrimitive.content.toFloat(),
        getValue(JSON_Y).jsonPrimitive.content.toFloat(),
        getValue(JSON_RADIUS).jsonPrimitive.content.toFloat(),
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing circle JSON '$this'!", iae)
    null
}

// endregion

// region JRect

private const val JSON_LEFT = "left"
private const val JSON_TOP = "top"
private const val JSON_RIGHT = "right"
private const val JSON_BOTTOM = "bottom"

fun JRect.toJsonK() = buildJsonObject {
    putK(JSON_LEFT, left)
    putK(JSON_TOP, top)
    putK(JSON_RIGHT, right)
    putK(JSON_BOTTOM, bottom)
    putK(JSON_FORM_TYPE, formType)
}

fun JsonObject.toJRect() = try {
    JRect(
        getValue(JSON_LEFT).jsonPrimitive.content.toFloat(),
        getValue(JSON_TOP).jsonPrimitive.content.toFloat(),
        getValue(JSON_RIGHT).jsonPrimitive.content.toFloat(),
        getValue(JSON_BOTTOM).jsonPrimitive.content.toFloat(),
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(JPaths.LOG_TAG, "Error while parsing rectangle JSON '$this'!", iae)
    null
}

// endregion

// endregion

// region JRotation

private const val JSON_DEGREES = "degrees"
private const val JSON_PX = "px"
private const val JSON_PY = "py"

fun JRotation.toJsonK() = buildJsonObject {
    putK(JSON_DEGREES, degrees)
    putK(JSON_PX, px)
    putK(JSON_PY, py)
}

fun JsonObject?.toJRotation() = this?.let {
    try {
        JRotation(
            getValue(JSON_DEGREES).jsonPrimitive.float,
            getValue(JSON_PX).jsonPrimitive.float,
            getValue(JSON_PY).jsonPrimitive.float,
        )
    } catch (iae: IllegalArgumentException) {
        MTLogK.w(JPaths.LOG_TAG, "Error while parsing rotation JSON '$this'!", iae)
        throw iae
    }
}

// endregion
