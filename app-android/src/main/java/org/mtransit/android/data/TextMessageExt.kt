package org.mtransit.android.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.getDSTypeIdFromJson
import org.mtransit.android.commons.data.getScoreFromJson
import org.mtransit.android.commons.data.optDefaultPOIInit
import org.mtransit.android.commons.data.toJsonCommon
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.data.TextMessage.LOG_TAG
import kotlinx.serialization.json.put as putK

fun makeTextMessage(
    messageId: Long,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int,
    message: String,
    score: Int? = null,
    @Accessibility.AccessibilityType accessible: Int = Accessibility.DEFAULT,
) = TextMessage(
    messageId,
    dataSourceTypeId,
    message,
).apply {
    optDefaultPOIInit(
        score = score,
        accessible = accessible
    )
}

private const val JSON_MESSAGE = "message"
private const val JSON_MESSAGE_ID = "messageId"

fun TextMessage.toJsonK() = jsonRunCatching {
    buildJsonObject {
        putK(JSON_MESSAGE_ID, this@toJsonK.messageId)
        putK(JSON_MESSAGE, this@toJsonK.message)
        toJsonCommon(this@toJsonK)
    }
}.getOrThrow()

@Suppress("unused")
fun JsonObject.toTextMessage() = jsonRunCatching(LOG_TAG) {
    makeTextMessage(
        messageId = this.getValue(JSON_MESSAGE_ID).jsonPrimitive.long,
        dataSourceTypeId = this.getDSTypeIdFromJson(),
        message = this.getValue(JSON_MESSAGE).jsonPrimitive.content,
        score = getScoreFromJson(),
    )
}.getOrThrow()
