package org.mtransit.android.data

import android.graphics.Paint
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

class JPathsExtTest {

    @Test
    fun serializesAndRestoresPathForms() {
        val paths = JPaths("logo").apply {
            addPath(
                JPaths.JPath(
                    JPaths.JPaint(Paint.Style.STROKE, 2.5f),
                    JPaths.JCircle(1f, 2f, 3f),
                    JPaths.JRotation(4f, 5f, 6f),
                )
            )
            addPath(
                JPaths.JPath(
                    JPaths.JPaint(Paint.Style.FILL),
                    JPaths.JRect(1f, 2f, 3f, 4f),
                )
            )
        }

        val json = paths.toJson()

        assertEquals(2, json.getValue("paths").jsonArray.size)
        assertEquals(paths, json.toJPaths())
    }
}
