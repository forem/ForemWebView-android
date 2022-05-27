package com.forem.android.presentation.databinding

import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.forem.android.R
import com.google.android.material.imageview.ShapeableImageView

/** Holds all custom binding adapters that bind to [ShapeableImageView]. */
object ShapeableImageViewBindingAdapters {

    /**
     * Allows binding drawables to [ShapeableImageView] via "android:src".
     * Reference: https://stackoverflow.com/a/35809319/3689782 .
     */
    @BindingAdapter("android:src")
    @JvmStatic
    fun setImageDrawable(shapeableImageView: ShapeableImageView, imageUrl: String?) {
        val requestOptions = RequestOptions().placeholder(R.drawable.forem_icon_placeholder)
        Glide.with(shapeableImageView.context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(shapeableImageView)
    }
}
