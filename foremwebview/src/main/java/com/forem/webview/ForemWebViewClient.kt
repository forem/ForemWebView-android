package com.forem.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.MailTo
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.URL

@SuppressLint("DefaultLocale")
class ForemWebViewClient(
    private val activity: Activity?,
    private val webView: WebView,
    private val oauthWebView: WebView?,
    originalUrl: String,
    private val needsForemMetaData: Boolean,
    private val updateForemData: (foremUrl: String, title: String, logo: String) -> Unit,
    private val onPageFinish: () -> Unit,
    private val loadOauthWebView: (url: String) -> Unit,
    private val destroyOauthWebView: () -> Unit,
    private val canGoBackOrForward: (canGoBackStatus: Boolean, canGoForwardStatus: Boolean) -> Unit
) : WebViewClient() {

    private var baseUrl = ""

    init {
        setBaseUrl(originalUrl)
    }

    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    private var clearHistory = false

    private var userId: Int = -1

    private var token: String? = null

    fun foremMetaDataReceived(foremMetaDataMap: Map<String, String>) {
        if (UrlChecks.checkUrlIsCorrect(baseUrl)) {
            val host = URL(baseUrl).host
            val foremHomePageUrl = UrlChecks.checkAndAppendHttpsToUrl(host)
            updateForemData(
                foremHomePageUrl,
                foremMetaDataMap["title"] ?: "",
                foremMetaDataMap["logo"] ?: ""
            )
        }
    }

    fun userLoggedIn(userId: Int) {
        this.userId = userId
        generateFirebaseToken()
    }

    fun userLoggedOut() {
        unregisterDevice()
    }

    fun clearHistory() {
        clearHistory = true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (clearHistory) {
            clearHistory = false
            view?.clearHistory()
        }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        onPageFinish()
        view.visibility = View.VISIBLE

        if (needsForemMetaData) {
            // This javascript function triggers the [processForemMetaData] function in
            // [AndroidWebViewBridge]. This is needed to get the forem name and logo.
            val fetchMetaDataJSFunction =
                """
                    javascript:window.${BuildConfig.ANDROID_BRIDGE}.processForemMetaData((
                    function (){
                        var metas = document.getElementsByTagName('meta');
                        let foremMetaData = new Map();
                        for (var i=0; i<metas.length; i++) {
                            if (metas[i].getAttribute("property") == "forem:name") {
                                let title = metas[i].getAttribute("content");
                                foremMetaData.set("title", title);
                            }
                            else if (metas[i].getAttribute("property") == "forem:logo") {
                                let logo = metas[i].getAttribute("content");
                                foremMetaData.set("logo", logo);
                            }
                        }
                        var obj = Object.fromEntries(foremMetaData);
                        var jsonString = JSON.stringify(obj);
                        return jsonString;
                    }
                    )() );
                """.trimIndent()

            view.loadUrl(fetchMetaDataJSFunction)
        }
        super.onPageFinished(view, url)
    }

    private fun generateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                token = task.result
                if (token != null) {
                    registerDevice()
                }
            }
        }
    }

    private fun registerDevice() {
        if (token.isNullOrEmpty()) {
            return
        }
        val javascript =
            "window.ForemMobile?.registerDeviceToken('$token', 'com.forem.app', 'Android')"
        webView.post {
            webView.evaluateJavascript(javascript) {
                return@evaluateJavascript
            }
        }
    }

    private fun unregisterDevice() {
        if (token.isNullOrEmpty()) {
            return
        }
        val javascript =
            "window.ForemMobile?.unregisterDeviceToken('$userId', '$token', 'com.forem.app', 'Android')"
        webView.post {
            webView.evaluateJavascript(javascript) {
                return@evaluateJavascript
            }
        }
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        canGoBackOrForward(
            view?.canGoBack() ?: false,
            view?.canGoForward() ?: false
        )

        super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val host = Uri.parse(baseUrl).host
        val url = request!!.url.toString().toLowerCase()

        // If sign-out, clear all data.
        if (view?.originalUrl == "$baseUrl/signout_confirm") {
            clearData(view)
            CookieManager.getInstance().removeAllCookies(null)
        }

        val urlType = UrlChecks.getURLType(url = url, host = host.toString())

        if (oauthWebView != null) {
            clearData(oauthWebView)
            destroyOauthWebView()
        }

        return when (urlType) {
            UrlType.HOST_LINK -> {
                // Open in webView
                false
            }
            UrlType.OVERRIDE_LINK -> {
                // Open in webView
                false
            }
            UrlType.EMAIL_LINK -> {
                // Open using email app
                openEmailApp(view, url)
                true
            }
            UrlType.THIRD_PARTY_LINK -> {
                // Open using url/browser app
                openCustomTab(url)
                true
            }
            UrlType.OAUTH_LINK -> {
                // Open in oauthWebView
                loadOauthWebView(url)
                true
            }
        }
    }

    private fun openEmailApp(webView: WebView?, url: String) {
        val mailTo: MailTo = MailTo.parse(url)
        val intent = newEmailIntent(mailTo.to, mailTo.subject, mailTo.body, mailTo.cc)
        activity?.startActivity(
            Intent.createChooser(
                intent,
                activity.getString(R.string.send_email_with)
            )
        )
        webView?.reload()
    }

    private fun clearData(webView: WebView) {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
    }

    private fun openCustomTab(url: String) {
        if (activity != null) {
            CustomTabsIntent.Builder()
                .build()
                .also { it.launchUrl(activity, Uri.parse(url)) }
        }
    }

    fun sendBridgeMessage(type: BridgeMessageType, message: Map<String, Any>) {
        val payload = mutableMapOf<String, Any>()
        payload.putAll(message)
        payload["namespace"] = type.messageType
        val jsonMessage = JSONObject(payload as Map<*, *>).toString()
        val javascript = "window.ForemMobile?.injectJSMessage('$jsonMessage')"
        webView.post {
            webView.evaluateJavascript(javascript, null)
        }
    }

    private fun newEmailIntent(
        address: String?,
        subject: String?,
        body: String?,
        cc: String?
    ): Intent {

        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.putExtra(Intent.EXTRA_CC, cc)
        return intent
    }
}
