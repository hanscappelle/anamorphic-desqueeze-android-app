package be.hcpl.android.photofilters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LetterboxTest {
    @Test
    fun presetsHaveExpectedValuesAndLabels() {
        assertEquals(
            listOf(
                LetterboxRatioPreset(2.39f, R.string.ratio_2_39),
                LetterboxRatioPreset(1.85f, R.string.ratio_1_85),
                LetterboxRatioPreset(1.90f, R.string.ratio_1_90),
                LetterboxRatioPreset(1.66f, R.string.ratio_1_66),
                LetterboxRatioPreset(1.33f, R.string.ratio_letterbox_1_33),
            ),
            LETTERBOX_RATIO_PRESETS,
        )
    }

    @Test
    fun allPresetsProduceExpectedBarsOnSquareCanvas() {
        val expected = listOf(
            LetterboxBars(291, 291),
            LetterboxBars(229, 230),
            LetterboxBars(237, 237),
            LetterboxBars(199, 199),
            LetterboxBars(124, 124),
        )
        assertEquals(expected, LETTERBOX_RATIO_PRESETS.map { calculateLetterboxBars(1000, 1000, it.value) })
    }

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
