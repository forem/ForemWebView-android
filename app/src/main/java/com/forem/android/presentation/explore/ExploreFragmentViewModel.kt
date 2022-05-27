package com.forem.android.presentation.explore

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forem.android.app.model.CurrentForem
import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.repository.DiscoverForemRepository
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.utility.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreFragmentViewModel @Inject constructor(
    private val myForemRepository: MyForemRepository,
    discoverForemsRepository: DiscoverForemRepository
) : ViewModel() {

    val errorText = ObservableField("")

    val loadingViewVisibility = ObservableBoolean(false)

    val myForemCollection: LiveData<Resource<ForemCollection>> = myForemRepository.getMyForems()

    val currentForem: LiveData<Resource<CurrentForem>> = myForemRepository.getCurrentForem()

    /** API call to [DiscoverForemService] which gets received as LiveData. */
    val featuredForems: LiveData<Resource<ForemCollection>> =
        discoverForemsRepository.getDiscoverForems()

    fun deleteForem(forem: Forem) {
        viewModelScope.launch(Dispatchers.IO) {
            myForemRepository.deleteForem(forem)
        }
    }
}
