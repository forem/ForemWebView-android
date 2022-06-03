package com.foremlibrary.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.forem.webview.WebViewConstants
import com.forem.webview.WebViewFragment

private const val WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "MainActivity.WebViewFragment.resultListener"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadOrUpdateFragment("https://dev.to")
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

//                if (homeReached) {
//                    sendDataToFragmentResultListener(webViewNavigation = WebViewNavigation.HOME_REACHED)
//                } else {
//                    viewModel.backButtonEnabled.set(canGoBack)
//                    viewModel.forwardButtonEnabled.set(canGoForward)
//                }
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
}