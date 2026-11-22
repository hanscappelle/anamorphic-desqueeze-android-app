package be.hcpl.android.photofilters

import android.graphics.Bitmap
import androidx.media3.effect.Presentation

enum class SocialCropPreset(val width: Int, val height: Int, val labelRes: Int) {
    PORTRAIT_9_16(9, 16, R.string.crop_9_16),
    PORTRAIT_4_5(4, 5, R.string.crop_4_5),
    SQUARE_1_1(1, 1, R.string.crop_1_1),
}

internal data class CropBounds(val left: Int, val top: Int, val width: Int, val height: Int)

internal fun calculateCropBounds(width: Int, height: Int, preset: SocialCropPreset): CropBounds {
    require(width > 0 && height > 0) { "Image dimensions must be positive." }
    val cropWidth: Int
    val cropHeight: Int
    if (width.toLong() * preset.height > height.toLong() * preset.width) {
        cropWidth = (height.toLong() * preset.width / preset.height).toInt().coerceAtLeast(1)
        cropHeight = height
    } else {
        cropWidth = width
        cropHeight = (width.toLong() * preset.height / preset.width).toInt().coerceAtLeast(1)
    }
    return CropBounds((width - cropWidth) / 2, (height - cropHeight) / 2, cropWidth, cropHeight)
}

internal fun cropImage(bitmap: Bitmap, preset: SocialCropPreset): Bitmap {
    val bounds = calculateCropBounds(bitmap.width, bitmap.height, preset)
    return Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width, bounds.height)
}

internal fun socialCropEffect(preset: SocialCropPreset): Presentation =
    Presentation.createForAspectRatio(
        preset.width.toFloat() / preset.height,
        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
    )
