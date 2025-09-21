package be.hcpl.android.photofilters

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import be.hcpl.android.photofilters.MainViewModel.State
import be.hcpl.android.photofilters.domain.ConfigRepository

class MainViewModel(
    private val settings: ConfigRepository,
) : ViewModel() {

    val state = MutableLiveData<State>()

    init {
        state.postValue(
            State(
                lastUsedPosition = settings.lastUsedOption(),
                lastUsedRatio = settings.lastUsedRatio(),
            )
        )
    }

    fun updateLastUsedSelection(selection: Int) = settings.updateLastUsedOption(selection)

    fun updateLastUsedRatio(ratio: Float) = settings.updateLastUsedRatio(ratio)

    data class State(
        val lastUsedPosition: Int,
        val lastUsedRatio: Float,
    )

}