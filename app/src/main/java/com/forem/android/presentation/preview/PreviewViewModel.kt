package com.forem.android.presentation.preview

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forem.android.app.model.Forem
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.presentation.home.RouteToHome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val myForemRepository: MyForemRepository
) : ViewModel() {

    val loadingViewVisibility = ObservableBoolean(false)

    lateinit var routeToHome: RouteToHome

    val isAddButtonEnabled = ObservableField(false)

    val forem = ObservableField(Forem.getDefaultInstance())

    fun onAddToListButtonClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            myForemRepository.addToMyForems(forem.get()!!)
            myForemRepository.updateCurrentForem(forem.get()!!)
            withContext(Dispatchers.Main) {
                if (::routeToHome.isInitialized) {
                    routeToHome.routeToHome()
                }
            }
        }
    }
}
