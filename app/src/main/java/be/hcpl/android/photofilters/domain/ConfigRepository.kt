package be.hcpl.android.photofilters.domain

import be.hcpl.android.photofilters.R
import android.content.Context

const val KEY_LAST_OPTION = "last_option"
const val KEY_LAST_CUSTOM_RATIO = "last_custom_ratio"

class ConfigRepository(
    context: Context,
) {

    private val sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    fun lastUsedOption() = sharedPref.getInt(KEY_LAST_OPTION, 0)

    fun updateLastUsedOption(option: Int) = sharedPref.edit().putInt(KEY_LAST_OPTION, option).apply()

    fun lastUsedRatio() = sharedPref.getFloat(KEY_LAST_CUSTOM_RATIO, 1.33f)

    fun updateLastUsedRatio(ratio: Float) = sharedPref.edit().putFloat(KEY_LAST_CUSTOM_RATIO, ratio).apply()

}