package com.forem.android.presentation.databinding

import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter

/**
 * Holds all custom binding adapters that bind to [ImageView].
 */
object ImageViewBindingAdapters {
    /**
     * Binding adapter for setting tint color to [ImageView].
     */
    @BindingAdapter("app:tint")
    @JvmStatic
    fun setImageTint(view: ImageView, @ColorInt color: Int) {
        view.setColorFilter(color)
    }
}
