package com.forem.android.presentation.onboarding

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.repository.DiscoverForemRepository
import com.forem.android.utility.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** [ViewModel] to make API call for forem list.  */
@HiltViewModel
class OnboardingFragmentViewModel @Inject constructor(
    discoverForemsRepository: DiscoverForemRepository
) : ViewModel() {
    /** API call to [DiscoverForemService] which gets received as LiveData. */
    val featuredForems: LiveData<Resource<ForemCollection>> = discoverForemsRepository.getDiscoverForems()

    val loadingViewVisibility = ObservableBoolean(false)
}
