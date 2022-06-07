package com.forem.webview

public enum class UrlType {
    /** Links which are part of a particular forem. */
    HOST_LINK,
    /** Links which needs to be overridden to load in same webView. */
    OVERRIDE_LINK,
    /** Links which needs to be opened in oauthWebView with a different user agent. */
    OAUTH_LINK,
    /** Links which needs to be opened via email intent. */
    EMAIL_LINK,
    /** Links which are opened externally via third-party apps. */
    THIRD_PARTY_LINK
}
