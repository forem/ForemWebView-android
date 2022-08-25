package com.forem.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.forem.webview.utils.NetworkConnectionLiveData
import com.forem.webview.utils.WebViewStatus
import java.net.URL

/** Displays forem instance in a fragment. */
class WebViewFragment : Fragment(), FileChooserListener {

    companion object {
        private const val RESULT_LISTENER_KEY = "WebViewFragment.resultListener"
        private const val URL_ARGUMENT_KEY = "WebViewFragment.url"
        private const val NEEDS_FOREM_META_DATA = "WebViewFragment.needsForemMetaData"

        /**
         * Creates a new instance of [WebViewFragment].
         *
         * @param url of the forem instance.
         * @param resultListenerKey is a key required to send data back to
         *     [setFragmentResultListener].
         * @param needsForemMetaData determines whether [WebView] needs to load the metadata like
         *     name, logo, etc and send it back to the parent. This needs to be true when the
         *     user has tried to add forem instance by adding a domain name so that we can get
         *     other information like name, logo, etc.
         * @return a new instance of [WebViewFragment].
         */
        fun newInstance(
            url: String,
            resultListenerKey: String,
            needsForemMetaData: Boolean = false
        ): WebViewFragment {
            val fragment = WebViewFragment()
            val args = Bundle()
            args.putString(URL_ARGUMENT_KEY, url)
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putBoolean(NEEDS_FOREM_META_DATA, needsForemMetaData)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var foremWebViewSession: ForemWebViewSession

    private lateinit var resultListenerKey: String

    private lateinit var webViewBridge: AndroidWebViewBridge
    private lateinit var webViewClient: ForemWebViewClient
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private var baseUrl = ""

    private var noInternetConnectionContainer: FrameLayout? = null
    private var oauthWebViewContainer: FrameLayout? = null
    private var oauthWebView: WebView? = null
    private var webView: WebView? = null

    /** Provides an observable which can reflect the current status of WebView. */
    val currentWebViewStatus = MutableLiveData(WebViewStatus.LOADING)

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null && result.data?.data != null) {
                result.data?.data?.let {
                    filePathCallback?.onReceiveValue(arrayOf(it))
                }
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.web_view_fragment, container, false)

        currentWebViewStatus.value = WebViewStatus.LOADING

        noInternetConnectionContainer = view.findViewById(R.id.no_internet_connection_container)
        webView = view.findViewById(R.id.web_view)
        oauthWebViewContainer = view.findViewById(R.id.oauth_web_view_container)

        val args = requireArguments()
        baseUrl = args.getString(URL_ARGUMENT_KEY)!!
        val needsForemMetaData = args.getBoolean(NEEDS_FOREM_META_DATA)
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!

        noInternetConnectionContainer?.visibility = View.GONE

        setupWebView(baseUrl, needsForemMetaData)
        setupNetworkObserver()

        return view
    }

    private fun setupNetworkObserver() {
        NetworkConnectionLiveData(this.requireContext()).observe(
            this.requireActivity(),
            Observer { isConnected ->

                // Reload the webview before removing the no-internet container.
                if (noInternetConnectionContainer?.visibility == View.VISIBLE) {
                    if (isConnected) {
                        refresh()
                    }
                }

                noInternetConnectionContainer?.visibility = if (isConnected) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                return@Observer
            }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(baseUrl: String, needsForemMetaData: Boolean) {
        // User Agent
        val defaultUserAgent = webView!!.settings.userAgentString
        val extensionUserAgent = if (baseUrl != WebViewConstants.PASSPORT_URL) {
            BuildConfig.FOREM_AGENT_EXTENSION
        } else {
            BuildConfig.PASSPORT_AGENT_EXTENSION
        }

        webView!!.loadUrl(baseUrl)

        // WebView Settings
        webViewSettings(webView!!)
        webView!!.settings.userAgentString = "$defaultUserAgent $extensionUserAgent"

        // WebView Client
        webViewClient = ForemWebViewClient(
            activity = this.activity,
            webView = webView!!,
            oauthWebView = oauthWebView,
            originalUrl = baseUrl,
            needsForemMetaData = needsForemMetaData,
            updateForemData = { foremUrl, title, logo ->
                sendForemMetaDataToFragmentResultListener(
                    foremUrl,
                    title,
                    logo
                )
            },
            onPageFinish = {
                currentWebViewStatus.value = WebViewStatus.SUCCESSFUL
            },
            loadOauthWebView = { url -> loadOauthWebView(url) },
            destroyOauthWebView = { destroyOauthWebView() },
            canGoBackOrForward = { canGoBackStatus, canGoForwardStatus ->
                canGoBackOrForward(canGoBackStatus, canGoForwardStatus)
            }
        )
        webView!!.webViewClient = webViewClient

        // Javascript Interface
        webViewBridge = AndroidWebViewBridge(this.requireActivity(), webViewClient)
        webView!!.addJavascriptInterface(webViewBridge, BuildConfig.ANDROID_BRIDGE)

        foremWebViewSession = ForemWebViewSession.getInstance()
        foremWebViewSession.setAndroidWebViewBridge(webViewBridge)

        // WebView Chrome Client
        webView!!.webChromeClient = ForemWebChromeClient(fileChooserListener = this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadOauthWebView(oauthUrl: String) {
        if (oauthWebView == null) {
            createNewOauthWebViewInstance()
        }
        webView!!.visibility = View.GONE

        oauthWebViewContainer!!.visibility = View.VISIBLE
        oauthWebView?.loadUrl(oauthUrl)
    }

    private fun createNewOauthWebViewInstance() {
        oauthWebView = WebView(this.requireContext())
        oauthWebViewContainer!!.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        oauthWebViewContainer!!.addView(oauthWebView)

        // WebView Settings
        webViewSettings(oauthWebView!!)

        // Solutions that worked
        // 1. UserAgent = "Chrome/97.0.4692.98 Mobile"
        //      Reference: https://stackoverflow.com/a/47765912
        // 2. UserAgent = "Dalvik/2.1.0 (Linux; U; Android 11; moto g(60) Build/RRI31.Q1-42-51-12)"
        //      Reference: https://stackoverflow.com/a/57918590
        // 3. UserAgent = "Mozilla/5.0 (Linux; Android 11; moto g(60) Build/RRI31.Q1-42-51-12) (KHTML, like Gecko) Version/4.0 Chrome/97.0.4692.98 Mobile"
        //      Reference: Guess work only.
        oauthWebView!!.settings.userAgentString = System.getProperty("http.agent")

        // WebView Client
        oauthWebView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request!!.url.toString()
                val urlType = UrlChecks.getURLType(
                    url = url,
                    host = URL(baseUrl).host.toString()
                )
                return when (urlType) {
                    UrlType.OAUTH_LINK -> {
                        // Open this url in oauthWebView.
                        false
                    }
                    else -> {
                        // Open this url in webView.
                        webView!!.loadUrl(url)
                        destroyOauthWebView()
                        false
                    }
                }
            }
        }
    }

    private fun destroyOauthWebView() {
        oauthWebViewContainer!!.removeAllViews()
        oauthWebViewContainer!!.visibility = View.GONE
        oauthWebView = null

        webView!!.visibility = View.VISIBLE
    }

    private fun canGoBackOrForward(canGoBack: Boolean, canGoForward: Boolean) {
        sendDataToFragmentResultListener(canGoBack = canGoBack, canGoForward = canGoForward)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun webViewSettings(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.savePassword = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>>?) {
        this.filePathCallback = filePathCallback

        val galleryIntent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_PICK
        }
        val intentChooser = Intent.createChooser(
            galleryIntent,
            resources.getString(R.string.select_picture)
        )
        imagePickerLauncher.launch(intentChooser)
    }

    /**
     * This function handles all back navigation for WebViews.
     * Back navigation can be done by using the `back` icon at bottom of screen or
     * by native android back button.
     *
     * For `back` icon we do not close the app if the WebView cannot go back any further.
     * For native android back button we close the app if the WebView cannot go back any further.
     * This is controlled via `canExitApp` boolean.
     *
     * Along with this the function also takes care of webView and oauthWebView navigation.
     */
    private fun centrallyHandleBackNavigation(canExitApp: Boolean) {
        if (oauthWebView != null && oauthWebViewContainer!!.visibility == View.GONE) {
            // Edge Case: Ideally this case should never arise.
            destroyOauthWebView()
            if (webView!!.canGoBack()) {
                webView!!.goBack()
            } else {
                if (canExitApp) {
                    handleHomePageReached()
                }
            }
        } else if (oauthWebView != null && oauthWebViewContainer!!.visibility == View.VISIBLE) {
            // Case where oauthWebView is active.
            if (oauthWebView!!.canGoBack()) {
                oauthWebView?.goBack()
            } else {
                destroyOauthWebView()
            }
        } else if (oauthWebView == null && webView!!.canGoBack()) {
            // Case where oauthWebView is fully inactive.
            webView!!.goBack()
        } else {
            if (canExitApp) {
                handleHomePageReached()
            }
        }
    }

    /**
     * Function which gets called whenever the back button is pressed either android app to move
     * to previous webpage or exit the app.
     */
    fun onBackPressedFragment() {
        centrallyHandleBackNavigation(canExitApp = true)
    }

    private fun handleHomePageReached() {
        sendDataToFragmentResultListener(homeReached = true)
    }

    /**
     * Navigates back in webview history.
     */
    fun navigateBack() {
        centrallyHandleBackNavigation(canExitApp = false)
    }

    /**
     * Navigates forward in webview history.
     */
    fun navigateForward() {
        if (webView!!.canGoForward()) {
            webView!!.goForward()
        }
    }

    /**
     * Updates the forem instance with new url and loads in WebView.
     */
    fun updateForemInstance(baseUrl: String) {
        if (::webViewClient.isInitialized) {
            currentWebViewStatus.value = WebViewStatus.LOADING
            webViewClient.clearHistory()
            this.baseUrl = baseUrl
            webView?.loadUrl(baseUrl)
            webViewClient.setBaseUrl(baseUrl)
        }
    }

    /**
     * Refreshes the current forem instance.
     */
    fun refresh() {
        if (webView != null && webView!!.isVisible) {
            currentWebViewStatus.value = WebViewStatus.LOADING
            webView!!.reload()
        }

        if (oauthWebView != null && oauthWebView!!.isVisible) {
            currentWebViewStatus.value = WebViewStatus.LOADING
            oauthWebView!!.reload()
        }
    }

    private fun sendDataToFragmentResultListener(
        homeReached: Boolean = false,
        canGoBack: Boolean = false,
        canGoForward: Boolean = false
    ) {
        // Note: This fragmentManager check is needed to fix the following error:
        // java.lang.IllegalStateException: Fragment not associated with a fragment manager.
        // Steps to reproduce this error:
        // 1. Install the fresh copy of app.
        // 2. Add DEV community forem.
        // 3. Now go to explore section and add any other forem.
        // 4. On home screen click on forem name.
        // This leads to error in setFragmentResult as mentioned above.
        this.fragmentManager ?: return

        val bundle = Bundle()
        bundle.putBoolean(WebViewConstants.WEB_VIEW_CAN_GO_BACK_KEY, canGoBack)
        bundle.putBoolean(WebViewConstants.WEB_VIEW_CAN_GO_FORWARD_KEY, canGoForward)
        bundle.putBoolean(WebViewConstants.WEB_VIEW_HOME_REACHED_KEY, homeReached)
        setFragmentResult(resultListenerKey, bundle)
    }

    // This function should be used only if parent is [PreviewFragment].
    private fun sendForemMetaDataToFragmentResultListener(
        foremUrl: String,
        title: String,
        logo: String
    ) {
        val bundle = Bundle()
        bundle.putString(WebViewConstants.FOREM_META_DATA_URL_KEY, foremUrl)
        bundle.putString(WebViewConstants.FOREM_META_DATA_TITLE_KEY, title)
        bundle.putString(WebViewConstants.FOREM_META_DATA_LOGO_KEY, logo)
        setFragmentResult(WebViewConstants.FOREM_META_DATA_RESULT_LISTENER_KEY, bundle)
    }
}
