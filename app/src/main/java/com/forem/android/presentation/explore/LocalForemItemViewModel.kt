package com.forem.android.presentation.explore

import androidx.lifecycle.ViewModel
import com.forem.android.app.model.Forem

/** [ViewModel] for displaying local forem item in [ExploreFragment]. */
class LocalForemItemViewModel(
    val forem: Forem,
    private val position: Int,
    private val foremSelectedListener: ForemSelectedListener,
    private val deleteForemViaAccessibilityListener: DeleteForemViaAccessibilityListener,
    val isAccessibilityEnabled: Boolean
) : ExploreItemViewModel() {

    fun onForemClicked() {
        foremSelectedListener.onForemSelected(forem)
    }

    fun onDeleteForemClicked() {
        deleteForemViaAccessibilityListener.onDeleteForemClicked(forem, position)
    }
}
