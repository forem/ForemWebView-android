package com.forem.android.presentation.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.forem.android.R
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.HomeFragmentResult
import com.forem.android.app.model.WebViewNavigation
import com.forem.android.databinding.HomeActivityBinding
import com.forem.android.presentation.explore.ExploreActivity
import com.forem.android.presentation.onboarding.OnboardingActivity
import com.forem.android.presentation.preview.PreviewFragment
import com.forem.android.utility.NetworkConnectionLiveData
import com.forem.android.utility.getProto
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

private const val HOME_FRAGMENT_RESULT_LISTENER_KEY =
    "HomeActivity.HomeFragment.resultListener"
private const val HOME_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "HomeActivity.HomeFragment.resultListenerBundle"

/** Activity which displays [WebViewFragment]. */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), WebViewNavigationInterface {

    companion object {
        private const val DEEP_LINK_URL_EXTRA = "HomeActivity.deepLinkUrl"
        fun newInstance(context: Context, deepLinkUrl: String? = null): Intent {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(DEEP_LINK_URL_EXTRA, deepLinkUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    private lateinit var homeFragment: HomeFragment
    private lateinit var previewFragment: PreviewFragment

    private lateinit var binding: HomeActivityBinding
    private var snackbar: Snackbar? = null

    private val previousNetworkState = ObservableField(true)

    private var deepLinkUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.home_activity)

        val data = intent.getStringExtra("data")
        deepLinkUrl = if (!data.isNullOrEmpty()) {
            // Data from background notification via FCM.
            val jsonObject = JSONObject(data)
            jsonObject.optString("url")
        } else {
            // Data from other activities via web banner deep link.
            intent.getStringExtra(DEEP_LINK_URL_EXTRA)
        }

        if (savedInstanceState == null) {
            replaceFragment(createHomeFragment(deepLinkUrl))
        }

        setupNetworkObserver()
    }

    override fun onResume() {
        super.onResume()
        createHomeFragmentResultListener()
    }

    override fun onBackPressed() {
        if (::homeFragment.isInitialized && homeFragment.isVisible) {
            homeFragment.onBackPressedFragment()
        } else {
            super.onBackPressed()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val tag: String = fragment::class.java.name
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_placeholder, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun createHomeFragment(deepLinkUrl: String?): HomeFragment {
        homeFragment = HomeFragment.newInstance(
            deepLinkUrl,
            HOME_FRAGMENT_RESULT_LISTENER_KEY,
            HOME_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return homeFragment
    }

    private fun createHomeFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            HOME_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val homeFragmentResult = bundle.getProto(
                HOME_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                HomeFragmentResult.getDefaultInstance()
            )

            val destination = homeFragmentResult.destinationScreen
            routeToDestinationScreen(destination)

            val webViewNavigation = homeFragmentResult.webViewNavigation
            handleWebViewNavigation(webViewNavigation)
        }
    }

    private fun routeToDestinationScreen(destinationScreen: DestinationScreen?) {
        if (destinationScreen == null) {
            return
        }
        when (destinationScreen) {
            DestinationScreen.UNSPECIFIED_SCREEN -> {}
            DestinationScreen.EXPLORE -> startActivity(ExploreActivity.newInstance(this))
            DestinationScreen.ONBOARDING -> {
                val intent = OnboardingActivity.newInstance(context = this)
                startActivity(intent)
                finish()
            }
            DestinationScreen.PASSPORT -> {}
            DestinationScreen.ADD_FOREM -> {}
            DestinationScreen.PREVIEW -> {}
            DestinationScreen.HOME -> {}
            DestinationScreen.CLOSE_CURRENT -> onBackPressed()
            DestinationScreen.UNRECOGNIZED -> {}
        }
    }

    private fun handleWebViewNavigation(webViewNavigation: WebViewNavigation?) {
        if (webViewNavigation != null) {
            when (webViewNavigation) {
                WebViewNavigation.UNSPECIFIED_NAVIGATION -> {}
                WebViewNavigation.FORWARD -> {}
                WebViewNavigation.BACK -> {}
                WebViewNavigation.HOME_REACHED -> {
                    onWebPageHomeReached()
                }
                WebViewNavigation.UNRECOGNIZED -> {
                    // TODO(#176): Log this error
                }
            }
        }
    }

    private fun setupNetworkObserver() {
        NetworkConnectionLiveData(this).observe(
            this,
            Observer { isConnected ->
                // This check is needed to make sure that when app goes in pause/resume state
                // it does not call `showNetworkMessage` function again which primarily fails the
                // picture selection from gallery.
                if (previousNetworkState.get()!! != isConnected) {
                    showNetworkMessage(isConnected)
                    previousNetworkState.set(isConnected)
                }
                return@Observer
            }
        )
    }

    private fun showNetworkMessage(isConnected: Boolean) {
        if (!isConnected) {
            snackbar = Snackbar.make(
                binding.fragmentPlaceholder,
                R.string.network_not_reachable,
                Snackbar.LENGTH_LONG
            )
            snackbar?.duration = BaseTransientBottomBar.LENGTH_INDEFINITE

            val snackBarView: View = snackbar!!.view
            snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.internet_error_snackbar_background
                )
            )
            val textView = snackBarView.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text
            )
            textView.setTextColor(ContextCompat.getColor(this, R.color.primary_text_color))
            val typeface = ResourcesCompat.getFont(this, R.font.epilogue_regular)
            textView.typeface = typeface

            snackbar?.show()
        } else {
            if (::previewFragment.isInitialized && previewFragment.isVisible) {
                previewFragment.refresh()
            }
            if (::homeFragment.isInitialized && homeFragment.isVisible) {
                homeFragment.refresh()
            }
            snackbar?.dismiss()
        }
    }

    override fun onWebPageHomeReached() {
        finish()
    }
}
