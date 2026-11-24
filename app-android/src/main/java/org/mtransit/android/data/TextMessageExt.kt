package org.mtransit.android.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.applyTo
import org.mtransit.android.commons.data.getDSTypeIdFromJson
import org.mtransit.android.commons.data.toJsonCommon
import org.mtransit.android.data.TextMessage.LOG_TAG
import kotlinx.serialization.json.put as putK

private const val JSON_MESSAGE = "message"
private const val JSON_MESSAGE_ID = "messageId"

fun TextMessage.toJsonK() = try {
    buildJsonObject {
        putK(JSON_MESSAGE_ID, this@toJsonK.messageId)
        putK(JSON_MESSAGE, this@toJsonK.message)
        toJsonCommon(this@toJsonK)
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", iae)
    throw iae
}

@Suppress("unused")
fun JsonObject.toTextMessage() = try {
    TextMessage(
        this.getValue(JSON_MESSAGE_ID).jsonPrimitive.long,
        this.getDSTypeIdFromJson(),
        this.getValue(JSON_MESSAGE).jsonPrimitive.content,
    ).apply {
        applyTo(this)
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    throw iae
}
