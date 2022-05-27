package com.forem.webview

import android.webkit.WebView
import androidx.databinding.BindingAdapter

/** Holds all custom binding adapters that bind to [WebView]. */
object WebViewBindingAdapters {

    /**
     * Allows binding url to an [WebView] via "foremUrl".
     * Reference: https://stackoverflow.com/a/57644267 .
     */
    @BindingAdapter("foremUrl")
    @JvmStatic
    fun WebView.updateUrl(url: String?) {
        url?.let {
            loadUrl(url)
        }
    }
}
