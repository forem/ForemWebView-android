package com.forem.android.presentation.addforem

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.utility.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddForemViewModel @Inject constructor(myForemRepository: MyForemRepository) : ViewModel() {

    val loadingViewVisibility = ObservableBoolean(false)

    val myForemCollection: LiveData<Resource<ForemCollection>> = myForemRepository.getMyForems()

    lateinit var nextButtonListener: NextButtonListener

    val addForemErrorMsg = ObservableField("")

    val userInputForem = ObservableField("")

    fun onNextButtonClicked() {
        nextButtonListener.onNextButtonClicked()
    }
}
