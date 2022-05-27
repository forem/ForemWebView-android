package com.forem.android.presentation.onboarding

import com.forem.android.presentation.addforem.RouteToAddForem

/** [ViewModel] for header in [OnboardingFragment]. */
class OnboardingHeaderItemViewModel(
    private val routeToAddForem: RouteToAddForem
) : OnboardingItemViewModel() {

    fun onAddForemClicked() {
        routeToAddForem.routeToAddForem()
    }
}
