package com.forem.android.presentation.explore

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
import com.forem.android.app.model.AddForemFragmentResult
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.ExploreFragmentResult
import com.forem.android.app.model.Forem
import com.forem.android.app.model.PassportFragmentResult
import com.forem.android.app.model.PreviewFragmentData
import com.forem.android.app.model.PreviewFragmentResult
import com.forem.android.app.model.WebViewNavigation
import com.forem.android.databinding.ExploreActivityBinding
import com.forem.android.presentation.addforem.AddForemFragment
import com.forem.android.presentation.home.WebViewNavigationInterface
import com.forem.android.presentation.passport.PassportFragment
import com.forem.android.presentation.preview.PreviewFragment
import com.forem.android.utility.NetworkConnectionLiveData
import com.forem.android.utility.getProto
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

private const val ADD_FOREM_FRAGMENT_RESULT_LISTENER_KEY =
    "ExploreActivity.AddForemFragment.resultListener"
private const val ADD_FOREM_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "ExploreActivity.AddForemFragment.resultListenerBundle"

private const val EXPLORE_FRAGMENT_RESULT_LISTENER_KEY =
    "ExploreActivity.ExploreFragment.resultListener"
private const val EXPLORE_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "ExploreActivity.ExploreFragment.resultListenerBundle"

private const val PASSPORT_FRAGMENT_RESULT_LISTENER_KEY =
    "ExploreActivity.PassportFragment.resultListener"
private const val PASSPORT_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "ExploreActivity.PassportFragment.resultListenerBundle"

private const val PREVIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "ExploreActivity.PreviewFragment.resultListener"
private const val PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "ExploreActivity.PreviewFragment.resultListenerBundle"

/**
 * Activity which displays [ExploreFragment] along with its child fragments i.e.
 * [AddForemFragment] and [PreviewFragment].
 */
@AndroidEntryPoint
class ExploreActivity : AppCompatActivity(), WebViewNavigationInterface {

    companion object {
        /**
         * Start [ExploreActivity] by creating a new instance using intents.
         *
         * @param context Context of parent/source activity.
         *
         * @return Returns Intent for [ExploreActivity]
         */
        fun newInstance(context: Context): Intent {
            return Intent(context, ExploreActivity::class.java)
        }
    }

    private lateinit var addForemFragment: AddForemFragment
    private lateinit var passportFragment: PassportFragment
    private lateinit var previewFragment: PreviewFragment
    private lateinit var exploreFragment: ExploreFragment

    private lateinit var binding: ExploreActivityBinding
    private var snackbar: Snackbar? = null

    private val previousNetworkState = ObservableField(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.explore_activity)

        if (savedInstanceState == null) {
            replaceFragment(createExploreFragment())
        }

        setupNetworkObserver()
    }

    override fun onResume() {
        super.onResume()
        // TODO(#176): Optimise these result listeners to attach as-per-need basis only.
        createExploreFragmentResultListener()
        createPassportFragmentResultListener()
        createAddForemFragmentResultListener()
        createPreviewFragmentResultListener()
    }

    override fun onBackPressed() {
        if (::exploreFragment.isInitialized && exploreFragment.isVisible) {
            // For removing the ExploreFragment.
            super.onBackPressed()
            // For finishing the activity.
            finish()
        } else if (::passportFragment.isInitialized && passportFragment.isVisible) {
            passportFragment.onBackPressedFragment()
        } else {
            super.onBackPressed()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val tag: String = fragment::class.java.name
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_placeholder, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun createExploreFragment(): ExploreFragment {
        exploreFragment = ExploreFragment.newInstance(
            EXPLORE_FRAGMENT_RESULT_LISTENER_KEY,
            EXPLORE_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return exploreFragment
    }

    private fun createPassportFragment(): PassportFragment {
        passportFragment = PassportFragment.newInstance(
            PASSPORT_FRAGMENT_RESULT_LISTENER_KEY,
            PASSPORT_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return passportFragment
    }

    private fun createAddForemFragment(): AddForemFragment {
        addForemFragment = AddForemFragment.newInstance(
            ADD_FOREM_FRAGMENT_RESULT_LISTENER_KEY,
            ADD_FOREM_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return addForemFragment
    }

    private fun createPreviewFragment(forem: Forem, deepLinkUrl: String?): PreviewFragment {
        previewFragment = PreviewFragment.newInstance(
            forem,
            deepLinkUrl,
            PREVIEW_FRAGMENT_RESULT_LISTENER_KEY,
            PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return previewFragment
    }

    private fun createExploreFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            EXPLORE_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val exploreFragmentResult = bundle.getProto(
                EXPLORE_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                ExploreFragmentResult.getDefaultInstance()
            )

            val previewFragmentData = exploreFragmentResult.previewFragmentData

            val destinationScreen = exploreFragmentResult.destinationScreen
            routeToDestinationScreen(destinationScreen, previewFragmentData)
        }
    }

    private fun createAddForemFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            ADD_FOREM_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val addForemFragmentResult = bundle.getProto(
                ADD_FOREM_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                AddForemFragmentResult.getDefaultInstance()
            )

            val previewFragmentData = addForemFragmentResult.previewFragmentData

            val destinationScreen = addForemFragmentResult.destinationScreen
            routeToDestinationScreen(destinationScreen, previewFragmentData)
        }
    }

    private fun createPassportFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            PASSPORT_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val passportFragmentResult = bundle.getProto(
                PASSPORT_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                PassportFragmentResult.getDefaultInstance()
            )

            val destinationScreen = passportFragmentResult.destinationScreen
            routeToDestinationScreen(destinationScreen)

            val webViewNavigation = passportFragmentResult.webViewNavigation
            handleWebViewNavigation(webViewNavigation)
        }
    }

    private fun createPreviewFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            PREVIEW_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val previewFragmentResult = bundle.getProto(
                PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                PreviewFragmentResult.getDefaultInstance()
            )

            val destinationScreen = previewFragmentResult.destinationScreen
            routeToDestinationScreen(destinationScreen)
        }
    }

    private fun routeToDestinationScreen(
        destinationScreen: DestinationScreen?,
        previewFragmentData: PreviewFragmentData? = PreviewFragmentData.getDefaultInstance()
    ) {
        if (destinationScreen == null) {
            return
        }
        when (destinationScreen) {
            DestinationScreen.UNSPECIFIED_SCREEN -> {}
            DestinationScreen.EXPLORE -> {}
            DestinationScreen.ONBOARDING -> {}
            DestinationScreen.PASSPORT -> replaceFragment(createPassportFragment())
            DestinationScreen.ADD_FOREM -> replaceFragment(createAddForemFragment())
            DestinationScreen.PREVIEW -> {
                if (previewFragmentData == null) {
                    return
                }
                replaceFragment(
                    createPreviewFragment(
                        previewFragmentData.forem,
                        previewFragmentData.deepLinkUrl
                    )
                )
            }
            DestinationScreen.HOME -> {
                finish()
            }
            DestinationScreen.CLOSE_CURRENT -> onBackPressed()
            DestinationScreen.UNRECOGNIZED -> {
                // TODO(#176): Log this error
            }
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

    override fun onWebPageHomeReached() {
        if (::exploreFragment.isInitialized && exploreFragment.isVisible) {
            finish()
        } else if (::passportFragment.isInitialized && passportFragment.isVisible) {
            // DO NOT CALL onBackPressed function otherwise this will end up in infinite loop.
            super.onBackPressed()
        } else {
            finish()
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
            if (::passportFragment.isInitialized && passportFragment.isVisible) {
                passportFragment.refresh()
            }
            if (::previewFragment.isInitialized && previewFragment.isVisible) {
                previewFragment.refresh()
            }
            snackbar?.dismiss()
        }
    }
}
