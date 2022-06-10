package com.forem.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/** Class which extends WebChromeClient to override all out-of WebView actions. */
class ForemWebChromeClient(
    private val fileChooserListener: FileChooserListener
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        fileChooserListener.openFileChooser(filePathCallback)
        return true
    }
}
