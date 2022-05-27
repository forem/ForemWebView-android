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
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.forem.webview.databinding.WebViewFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL
import javax.inject.Inject

/** Displays forem in fragment. */
@AndroidEntryPoint
class WebViewFragment : Fragment(), FileChooserListener {

    companion object {
        private const val RESULT_LISTENER_KEY = "WebViewFragment.resultListener"
        private const val URL_ARGUMENT_KEY = "WebViewFragment.url"
        private const val NEEDS_FOREM_META_DATA = "WebViewFragment.needsForemMetaData"

        /** Creates a new instance of [WebViewFragment]. */
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

    @Inject
    lateinit var foremWebViewSession: ForemWebViewSession

    private lateinit var resultListenerKey: String

    private lateinit var binding: WebViewFragmentBinding
    private lateinit var viewModel: WebViewFragmentViewModel

    private lateinit var webViewBridge: AndroidWebViewBridge
    private lateinit var webViewClient: ForemWebViewClient
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private var oauthWebView: WebView? = null

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
        val args = requireArguments()
        val baseUrl = args.getString(URL_ARGUMENT_KEY)!!
        val needsForemMetaData = args.getBoolean(NEEDS_FOREM_META_DATA)
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!

        viewModel = ViewModelProvider(this).get(WebViewFragmentViewModel::class.java)
        viewModel.baseUrl.set(baseUrl)

        binding = WebViewFragmentBinding.inflate(inflater, container, /* attachToRoot= */ false)
        binding.viewModel = viewModel

        setupWebView(baseUrl, needsForemMetaData)

        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(baseUrl: String, needsForemMetaData: Boolean) {
        // User Agent
        val defaultUserAgent = binding.webView.settings.userAgentString
        val extensionUserAgent = if (baseUrl != WebViewConstants.PASSPORT_URL)
            BuildConfig.FOREM_AGENT_EXTENSION
        else
            BuildConfig.PASSPORT_AGENT_EXTENSION

        // WebView Settings
        webViewSettings(binding.webView)
        binding.webView.settings.userAgentString = "$defaultUserAgent $extensionUserAgent"

        // Javascript Interface
        webViewBridge = AndroidWebViewBridge(this.requireActivity())
        foremWebViewSession.androidWebViewBridge = webViewBridge
        binding.webView.addJavascriptInterface(webViewBridge, BuildConfig.ANDROID_BRIDGE)

        // WebView Client
        webViewClient = ForemWebViewClient(
            activity = this.activity,
            webView = binding.webView,
            oauthWebView = oauthWebView,
            originalUrl = viewModel.baseUrl.get()!!,
            needsForemMetaData = needsForemMetaData,
            updateForemData = { foremUrl, title, logo ->
                sendForemMetaDataToFragmentResultListener(
                    foremUrl, title, logo
                )
            },
            onPageFinish = { viewModel.loadingViewVisibility.set(false) },
            loadOauthWebView = { url -> loadOauthWebView(url) },
            destroyOauthWebView = { destroyOauthWebView() },
            canGoBackOrForward = { canGoBackStatus, canGoForwardStatus ->
                canGoBackOrForward(canGoBackStatus, canGoForwardStatus)
            }
        )
        binding.webView.webViewClient = webViewClient
        webViewBridge.webViewClient = webViewClient

        // WebView Chrome Client
        binding.webView.webChromeClient = ForemWebChromeClient(fileChooserListener = this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadOauthWebView(oauthUrl: String) {
        if (oauthWebView == null) {
            createNewOauthWebViewInstance()
        }
        binding.webView.visibility = View.GONE

        binding.oauthWebViewContainer.visibility = View.VISIBLE
        oauthWebView?.loadUrl(oauthUrl)
    }

    private fun createNewOauthWebViewInstance() {
        oauthWebView = WebView(this.requireContext())
        binding.oauthWebViewContainer.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        binding.oauthWebViewContainer.addView(oauthWebView)

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
                    host = URL(viewModel.baseUrl.get()).host.toString()
                )
                return when (urlType) {
                    UrlType.OAUTH_LINK -> {
                        // Open this url in oauthWebView.
                        false
                    }
                    else -> {
                        // Open this url in webView.
                        binding.webView.loadUrl(url)
                        destroyOauthWebView()
                        false
                    }
                }
            }
        }
    }

    private fun destroyOauthWebView() {
        binding.oauthWebViewContainer.removeAllViews()
        binding.oauthWebViewContainer.visibility = View.GONE
        oauthWebView = null

        binding.webView.visibility = View.VISIBLE
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
        if (oauthWebView != null && binding.oauthWebViewContainer.visibility == View.GONE) {
            // Edge Case: Ideally this case should never arise.
            destroyOauthWebView()
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                if (canExitApp) {
                    handleHomePageReached()
                }
            }
        } else if (oauthWebView != null && binding.oauthWebViewContainer.visibility == View.VISIBLE) {
            // Case where oauthWebView is active.
            if (oauthWebView!!.canGoBack()) {
                oauthWebView?.goBack()
            } else {
                destroyOauthWebView()
            }
        } else if (oauthWebView == null && binding.webView.canGoBack()) {
            // Case where oauthWebView is fully inactive.
            binding.webView.goBack()
        } else {
            if (canExitApp) {
                handleHomePageReached()
            }
        }
    }

    fun onBackPressedFragment() {
        centrallyHandleBackNavigation(canExitApp = true)
    }

    private fun handleHomePageReached() {
        sendDataToFragmentResultListener(homeReached = true)
    }

    fun navigateBack() {
        centrallyHandleBackNavigation(canExitApp = false)
    }

    fun navigateForward() {
        if (binding.webView.canGoForward()) {
            binding.webView.goForward()
        }
    }

    fun updateForemInstance(baseUrl: String) {
        if (::webViewClient.isInitialized && ::viewModel.isInitialized) {
            webViewClient.clearHistory()
            viewModel.baseUrl.set(baseUrl)
            webViewClient.setBaseUrl(baseUrl)
        }
    }

    fun refresh() {
        binding.webView.reload()
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
