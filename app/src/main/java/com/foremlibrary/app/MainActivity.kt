package com.foremlibrary.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.forem.webview.WebViewConstants
import com.forem.webview.WebViewFragment

private const val WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "MainActivity.WebViewFragment.resultListener"

private const val DEV_LOCAL = "file:///android_asset/forem.dev.html"
private const val DEV_TO = "https://dev.to"
private const val MMA_LIFE = "https://www.thismmalife.com/"

/** Helper activity which connects with foremwebview module. */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val FOREM_URL_EXTRA = "MainActivity.forem_url"

        fun newInstance(context: Context, url: String?): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(FOREM_URL_EXTRA, url)
            return intent
        }
    }

    private lateinit var backImageView: ImageView
    private lateinit var forwardImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val url = intent?.getStringExtra(FOREM_URL_EXTRA) ?: ""

        if (url.isEmpty()) {
            loadOrUpdateFragment(DEV_TO)
        } else {
            loadOrUpdateFragment(url)
        }

        backImageView = findViewById(R.id.back_image_view)
        forwardImageView = findViewById(R.id.forward_image_view)

        backImageView.setOnClickListener {
            onWebViewBackClicked()
        }

        forwardImageView.setOnClickListener {
            onWebViewForwardClicked()
        }

        // Disabled by default
        backImageView.isEnabled = false
        forwardImageView.isEnabled = false
    }

    override fun onBackPressed() {
        getWebViewFragment()?.onBackPressedFragment()
    }

    private fun loadOrUpdateFragment(url: String) {
        if (getWebViewFragment() == null) {
            supportFragmentManager.setFragmentResultListener(
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                /* lifecycleOwner= */ this
            ) { _, bundle ->
                val canGoBack = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_BACK_KEY)
                val canGoForward = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_FORWARD_KEY)
                val homeReached = bundle.getBoolean(WebViewConstants.WEB_VIEW_HOME_REACHED_KEY)

                if (homeReached) {
                    onWebPageHomeReached()
                } else {
                    backImageView.isEnabled = canGoBack
                    forwardImageView.isEnabled = canGoForward
                }
            }

            val webViewFragment = WebViewFragment.newInstance(
                url,
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                needsForemMetaData = false
            )
            this.supportFragmentManager.beginTransaction().add(
                R.id.web_view_fragment,
                webViewFragment
            ).commit()
        } else {
            getWebViewFragment()?.updateForemInstance(url)
        }
    }

    private fun getWebViewFragment(): WebViewFragment? {
        return this.supportFragmentManager.findFragmentById(
            R.id.web_view_fragment
        ) as WebViewFragment?
    }

    private fun onWebViewBackClicked() {
        getWebViewFragment()?.navigateBack()
    }

    private fun onWebViewForwardClicked() {
        getWebViewFragment()?.navigateForward()
    }

    private fun onWebPageHomeReached() {
        finish()
        return
    }
}
