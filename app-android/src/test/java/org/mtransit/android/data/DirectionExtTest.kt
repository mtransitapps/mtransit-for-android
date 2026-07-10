package org.mtransit.android.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DirectionExtTest {

    @Test
    fun test_removeStartFromHeadingStarts() {
        ("East" to "East via A").let { (startsWith, heading) ->
            removeStartFromHeadingStarts(heading, startsWith)
        }.let { result ->
            assertEquals("via A", result)
        }
        ("North" to "North B").let { (startsWith, heading) ->
            removeStartFromHeadingStarts(heading, startsWith)
        }.let { result ->
            assertEquals("B", result)
        }
        ("South" to "South (Short)").let { (startsWith, heading) ->
            removeStartFromHeadingStarts(heading, startsWith)
        }.let { result ->
            assertEquals("(Short)", result)
        }
        ("Toulouse" to "Toulouse: Gare Routière").let { (startsWith, heading) ->
            removeStartFromHeadingStarts(heading, startsWith)
        }.let { result ->
            assertEquals("Gare Routière", result)
        }
    }
}
