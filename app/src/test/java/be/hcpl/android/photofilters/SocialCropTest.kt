package be.hcpl.android.photofilters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SocialCropTest {
    @Test
    fun landscapeCropsAreCenteredForEachPreset() {
        assertEquals(CropBounds(656, 0, 607, 1080), calculateCropBounds(1920, 1080, SocialCropPreset.PORTRAIT_9_16))
        assertEquals(CropBounds(528, 0, 864, 1080), calculateCropBounds(1920, 1080, SocialCropPreset.PORTRAIT_4_5))
        assertEquals(CropBounds(420, 0, 1080, 1080), calculateCropBounds(1920, 1080, SocialCropPreset.SQUARE_1_1))
    }

    @Test
    fun portraitCropsHeightWhenSourceIsNarrowerThanTarget() {
        assertEquals(CropBounds(0, 285, 1080, 1350), calculateCropBounds(1080, 1920, SocialCropPreset.PORTRAIT_4_5))
        assertEquals(CropBounds(0, 420, 1080, 1080), calculateCropBounds(1080, 1920, SocialCropPreset.SQUARE_1_1))
        assertEquals(CropBounds(0, 0, 1080, 1920), calculateCropBounds(1080, 1920, SocialCropPreset.PORTRAIT_9_16))
    }

    @Test
    fun handlesOddAndVerySmallDimensions() {
        assertEquals(CropBounds(2, 0, 5, 9), calculateCropBounds(10, 9, SocialCropPreset.PORTRAIT_9_16))
        assertEquals(CropBounds(0, 0, 1, 1), calculateCropBounds(1, 1, SocialCropPreset.PORTRAIT_9_16))
        assertThrows(IllegalArgumentException::class.java) {
            calculateCropBounds(0, 10, SocialCropPreset.SQUARE_1_1)
        }
    }
}
