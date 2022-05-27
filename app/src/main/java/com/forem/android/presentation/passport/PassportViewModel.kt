package com.forem.android.presentation.passport

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PassportViewModel @Inject constructor() : ViewModel() {
    val loadingViewVisibility = ObservableBoolean(false)
}
