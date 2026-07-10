package org.mtransit.android.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DirectionExtTest {

    @Test
    fun test_removeStartFromHeadingStarts() {
        ("North" to "North B").let { (direction, trip) ->
            removeStartFromHeadingStarts(heading = trip, startsWith = direction)
        }.let { result ->
            assertEquals("B", result)
        }
        ("South" to "South (Short)").let { (direction, trip) ->
            removeStartFromHeadingStarts(heading = trip, startsWith = direction)
        }.let { result ->
            assertEquals("(Short)", result)
        }
        ("Toulouse" to "Toulouse: Gare Routière").let { (direction, trip) ->
            removeStartFromHeadingStarts(heading = trip, startsWith = direction)
        }.let { result ->
            assertEquals("Gare Routière", result)
        }
    }
}
