package com.forem.webview

import android.net.Uri
import android.webkit.ValueCallback

/** Interface to open file chooser natively in android. */
interface FileChooserListener {
    /**
     * Function which passes the filePathCallback which gets used again once the images are selected
     * natively.
     */
    fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>>?)
}
