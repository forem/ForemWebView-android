package com.forem.android.presentation.databinding

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/**
 * Holds all custom binding adapters that bind to [TextInputLayout].
 */
object TextInputLayoutBindingAdapters {
    /**
     * Binding adapter for setting an error message.
     */
    @BindingAdapter("app:errorMessage")
    @JvmStatic
    fun setErrorMessage(textInputLayout: TextInputLayout, errorMessage: String?) {
        textInputLayout.error = errorMessage
    }
}
