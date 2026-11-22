package be.hcpl.android.photofilters

const val IMAGE_JPEG = "image/jpeg"
const val PREFIX = "Desqueezed"
const val EXT_JPEG = "JPEG"
const val INTENT_TYPE_ALL_IMAGE = "image/"
const val INTENT_TYPE_ALL_VIDEO = "video/"

const val JPEG_COMPRESSION = 85

// pre configured aspect ratio
const val ASPECT_RATIO_1_33 = 1.33f
const val ASPECT_RATIO_1_55 = 1.55f
const val ASPECT_RATIO_CUSTOM = 0f
const val ASPECT_RATIO_DEFAULT = ASPECT_RATIO_1_33

internal data class LetterboxRatioPreset(val value: Float, val labelRes: Int)

internal val LETTERBOX_RATIO_PRESETS = listOf(
    LetterboxRatioPreset(2.39f, R.string.ratio_2_39),
    LetterboxRatioPreset(1.85f, R.string.ratio_1_85),
    LetterboxRatioPreset(1.90f, R.string.ratio_1_90),
    LetterboxRatioPreset(1.66f, R.string.ratio_1_66),
    LetterboxRatioPreset(1.33f, R.string.ratio_letterbox_1_33),
)
