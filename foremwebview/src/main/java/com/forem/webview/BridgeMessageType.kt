package com.forem.webview

/** Enum to determine common actions within podcast and video. */
enum class BridgeMessageType(val messageType: String) {
    /** PODCAST enum. */
    PODCAST("podcast"),
    /** VIDEO enum. */
    VIDEO("video")
}
