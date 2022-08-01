package com.forem.webview

import android.content.Context
import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent

/** Class which extends WebChromeClient to override all out-of WebView actions. */
class ForemWebChromeClient(
    private val fileChooserListener: FileChooserListener
) : WebChromeClient() {

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val result = view!!.hitTestResult
        val data = result.extra
        openCustomTab(view.context, Uri.parse(data).toString())
        return false
    }

    private fun openCustomTab(context: Context?, url: String) {
        if (context != null) {
            CustomTabsIntent.Builder()
                .build()
                .also { it.launchUrl(context, Uri.parse(url)) }
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        fileChooserListener.openFileChooser(filePathCallback)
        return true
    }
}
