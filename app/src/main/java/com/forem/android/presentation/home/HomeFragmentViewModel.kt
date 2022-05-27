package com.forem.android.presentation.home

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forem.android.app.model.CurrentForem
import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.utility.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [ViewModel] to fetch local forem list and display forems. */
@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    private val myForemRepository: MyForemRepository
) : ViewModel() {

    val currentForemLiveData: LiveData<Resource<CurrentForem>> = myForemRepository.getCurrentForem()

    val myForemsLiveData: LiveData<Resource<ForemCollection>> = myForemRepository.getMyForems()

    val currentForem = ObservableField(Forem.getDefaultInstance())

    val forwardButtonEnabled = ObservableField(false)
    val backButtonEnabled = ObservableField(false)

    fun updateCurrentForem(forem: Forem) {
        viewModelScope.launch(Dispatchers.IO) {
            myForemRepository.updateCurrentForem(forem)
        }
    }
}
