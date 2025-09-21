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
                lastUsedRatio = settings.lastUsedRatio(),
            )
        )
    }

    fun updateLastUsedRatio(ratio: Float) = settings.updateLastUsedRatio(ratio)

    data class State(
        val lastUsedRatio: Float,
    )

}