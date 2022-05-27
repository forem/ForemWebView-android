package com.forem.android.presentation.onboarding

import com.forem.android.app.model.Forem
import com.forem.android.presentation.preview.RouteToPreview

/** [ViewModel] for displaying forem item in [OnboardingFragment]. */
class OnboardingForemItemViewModel(
    val forem: Forem,
    private val routeToPreview: RouteToPreview
) : OnboardingItemViewModel() {

    fun onForemClicked() {
        routeToPreview.routeToPreview(forem)
    }
}
