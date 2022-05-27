package com.forem.android.presentation.explore

import androidx.lifecycle.ViewModel
import com.forem.android.app.model.Forem

/** [ViewModel] for displaying forem item in [ExploreFragment]. */
class RemoteForemItemViewModel(
    val forem: Forem,
    private val foremSelectedListener: ForemSelectedListener
) : ExploreItemViewModel() {

    fun onForemClicked() {
        foremSelectedListener.onForemSelected(forem)
    }
}
