package com.forem.webview

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** [ViewModel] to fetch local forem list and display forems. */
@HiltViewModel
class WebViewFragmentViewModel @Inject constructor() : ViewModel() {
    val loadingViewVisibility = ObservableBoolean(true)

    val baseUrl = ObservableField("")
}
