package be.hcpl.android.photofilters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LetterboxTest {
    @Test
    fun cinematicRatiosKeepCanvasAndCenterVisiblePicture() {
        assertEquals(LetterboxBars(138, 139), calculateLetterboxBars(1920, 1080, 2.39f))
        assertEquals(LetterboxBars(131, 132), calculateLetterboxBars(1920, 1080, 2.35f))
        assertEquals(LetterboxBars(270, 270), calculateLetterboxBars(1080, 1080, 2f))
    }

    @Test
    fun matchingRatioNeedsNoBars() {
        assertEquals(LetterboxBars(0, 0), calculateLetterboxBars(1920, 1080, 16f / 9f))
    }

    @Test
    fun rejectsImpossibleOrInvalidRatios() {
        listOf(1f, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.MAX_VALUE).forEach { ratio ->
            assertThrows(InvalidLetterboxRatioException::class.java) {
                calculateLetterboxBars(1920, 1080, ratio)
            }
        }
    }
}
