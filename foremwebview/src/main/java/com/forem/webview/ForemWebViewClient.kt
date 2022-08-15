package com.forem.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.MailTo
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import org.json.JSONObject
import java.net.URL
import java.util.*

/**
 * Class which extends WebViewClient to override all functions and implement custom functionalities
 * within the WebView.
 */
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

    private var clearHistory = false

    private var userId: Int = -1

    private var token: String? = null

    private var foremMetaDataResult: String? = null

    init {
        setBaseUrl(originalUrl)
    }

    /**
     * Sets the base url.
     *
     * @param baseUrl is the url which is basically the host.
     */
    fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    /**
     * Function which gets called when user is logged-in successfully.
     *
     * @param userId unique user if associated with current user.
     */
    fun userLoggedIn(userId: Int) {
        this.userId = userId
        generateFirebaseToken()
    }

    /**
     * Gets called when user logs-out of the website.
     */
    fun userLoggedOut() {
        unregisterDevice()
    }

    /**
     * Function to set the clearHistory boolean to true.
     * @param
     */
    fun clearHistory() {
        clearHistory = true
    }

    /**
     * This function gets called from [AndroidWebViewBridge] whenever some information needs to be
     * sent to the website from the android device.
     *
     * @param type can be either podcast of video.
     * @param message contains the information along with actions which needs to be sent.
     */
    fun sendBridgeMessage(type: BridgeMessageType, message: Map<String, Any>) {
        val payload = mutableMapOf<String, Any>()
        payload.putAll(message)
        payload["namespace"] = type.messageType
        val jsonMessage = JSONObject(payload as Map<*, *>).toString()
        Log.d("TAG", "sendBridgeMessage: $jsonMessage")
        val javascript = "window.ForemMobile?.injectJSMessage('$jsonMessage')"
        webView.post {
            webView.evaluateJavascript(javascript, null)
        }
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

        if (needsForemMetaData && foremMetaDataResult == null) {
            getInstanceMetadata()
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

    private fun getInstanceMetadata() {
        val javascript = "window.ForemMobile?.getInstanceMetadata()"
        webView.post {
            webView.evaluateJavascript(javascript) {
                if (!it.isNullOrEmpty() && !it.equals("null")) {
                    foremMetaDataResult = it
                    processForemMetaData(it)
                }
                return@evaluateJavascript
            }
        }
    }

    private fun processForemMetaData(metaDataJSONStribg: String) {
        val foremMetaData = metaDataJSONStribg
            .replace("\"{", "{")
            .replace("}\"", "}")
            .replace("\\\"", "\"")
        val jsonObject = JSONObject(foremMetaData)
        var foremMetaDataMap: Map<String, String> = HashMap()
        foremMetaDataMap = Gson().fromJson(jsonObject.toString(), foremMetaDataMap.javaClass)
        foremMetaDataReceived(foremMetaDataMap)
    }

    private fun foremMetaDataReceived(foremMetaDataMap: Map<String, String>) {
        if (UrlChecks.checkUrlIsCorrect(baseUrl)) {
            val host = URL(baseUrl).host
            val foremHomePageUrl = UrlChecks.checkAndAppendHttpsToUrl(host)
            updateForemData(
                foremHomePageUrl,
                foremMetaDataMap["name"] ?: "",
                foremMetaDataMap["logo"] ?: ""
            )
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
        val url = request!!.url.toString().lowercase(Locale.getDefault())

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
