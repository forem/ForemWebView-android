package com.forem.webview

import android.net.Uri
import android.webkit.ValueCallback

interface FileChooserListener {
    fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>>?)
}
