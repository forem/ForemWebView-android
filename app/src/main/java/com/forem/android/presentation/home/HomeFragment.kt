package com.forem.android.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.forem.android.R
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import com.forem.android.app.model.HomeFragmentResult
import com.forem.android.app.model.WebViewNavigation
import com.forem.android.databinding.HomeFragmentBinding
import com.forem.android.utility.Resource
import com.forem.android.utility.putProto
import com.forem.webview.WebViewConstants
import com.forem.webview.WebViewFragment
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL

private const val WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "HomeFragment.WebViewFragment.resultListener"

private const val CLEAR_DEEPLINK_URL = ""

/** Displays forem list on home screen via API call */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        private const val DEEP_LINK_URL_ARGUMENT_KEY = "HomeFragment.deepLinkUrl"
        private const val RESULT_LISTENER_KEY = "HomeFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "HomeFragment.resultListenerBundle"

        /** Creates a new instance of [HomeFragment]. */
        fun newInstance(
            deepLinkUrl: String? = null,
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString(DEEP_LINK_URL_ARGUMENT_KEY, deepLinkUrl)
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: HomeFragmentBinding
    private lateinit var viewModel: HomeFragmentViewModel

    private var deepLinkUrl: String? = null
    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()

        // Why this if clause?
        // onCreateView gets called everyone the fragment becomes visible along with Bundle values,
        // as a result the deepLinkUrl is available available which then results in below
        // mentioned bug. To fix this we set change the deepLinkUrl during onPause such that
        // it gets reset and we can change the forem easily.
        //
        // Bug:
        // 1. Remove this if-clause and onPause() function.
        // 2. In SplashActivity set a valid deepLinkUrl for currently selected forem.
        // 3. Now run the app, open ExploreFragment, try to change the forem.
        // 4. Forem change is not possible.
        if (deepLinkUrl == null) {
            deepLinkUrl = args.getString(DEEP_LINK_URL_ARGUMENT_KEY)
        }
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        binding = HomeFragmentBinding.inflate(inflater, container, /* attachToRoot= */ false)
        viewModel = ViewModelProvider(this).get(HomeFragmentViewModel::class.java)

        binding.main = this
        binding.viewModel = viewModel

        setupObserver()
        addCustomAccessibilityActions()

        return binding.root
    }

    override fun onPause() {
        deepLinkUrl = CLEAR_DEEPLINK_URL
        super.onPause()
    }

    private fun addCustomAccessibilityActions() {
        ViewCompat.addAccessibilityAction(
            binding.webViewFragment,
            this.getString(R.string.back_content_description)
        ) { _, _ ->
            onWebViewBackClicked()
            true
        }

        ViewCompat.addAccessibilityAction(
            binding.webViewFragment,
            this.getString(R.string.forward_content_description)
        ) { _, _ ->
            onWebViewForwardClicked()
            true
        }

        ViewCompat.addAccessibilityAction(
            binding.webViewFragment,
            this.getString(R.string.my_forems_content_description)
        ) { _, _ ->
            onForemNameClicked()
            true
        }
    }

    private fun setupObserver() {
        viewModel.currentForemLiveData.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.data != null) {
                        if (deepLinkUrl.isNullOrEmpty()) {
                            viewModel.currentForem.set(it.data.forem!!)
                            loadOrUpdateFragment(it.data.forem!!)
                        } else {
                            // When deep-link is available.
                            val url = URL(deepLinkUrl)
                            if (it.data.forem!!.homePageUrl.contains(url.host)) {
                                viewModel.currentForem.set(it.data.forem!!)
                                loadOrUpdateFragment(it.data.forem!!)
                            } else {
                                getAllLocalForems()
                            }
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    // TODO(#175): Show proper UI for errors and log this error.
                }
                Resource.Status.LOADING -> {
                    // TODO(#175): Show loading screen
                }
            }
        }
    }

    private fun getAllLocalForems() {
        viewModel.myForemsLiveData.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.data == null || it.data.foremCount == 0 || deepLinkUrl.isNullOrEmpty()) {
                        openOnboardingActivity()
                    } else {
                        // Validate URL locally
                        val localForem: Forem? = validateForemLocally(it.data)
                        if (localForem != null) {
                            viewModel.updateCurrentForem(localForem)
                        } else {
                            openOnboardingActivity()
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    // TODO(#175): Show proper UI error message.
                }
                Resource.Status.LOADING -> {
                    // TODO(#175): Show loading screen
                }
            }
        }
    }

    private fun loadOrUpdateFragment(forem: Forem) {
        val url: String = if (deepLinkUrl == null || deepLinkUrl!!.isEmpty()) {
            forem.homePageUrl
        } else {
            deepLinkUrl!!
        }
        if (getWebViewFragment() == null) {
            childFragmentManager.setFragmentResultListener(
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                /* lifecycleOwner= */ this
            ) { _, bundle ->
                val canGoBack = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_BACK_KEY)
                val canGoForward = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_FORWARD_KEY)
                val homeReached = bundle.getBoolean(WebViewConstants.WEB_VIEW_HOME_REACHED_KEY)

                if (homeReached) {
                    sendDataToFragmentResultListener(webViewNavigation = WebViewNavigation.HOME_REACHED)
                } else {
                    viewModel.backButtonEnabled.set(canGoBack)
                    viewModel.forwardButtonEnabled.set(canGoForward)
                }
            }

            val webViewFragment = WebViewFragment.newInstance(
                url,
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                needsForemMetaData = false
            )
            this.childFragmentManager.beginTransaction().add(
                R.id.web_view_fragment,
                webViewFragment
            ).commit()
        } else {
            getWebViewFragment()?.updateForemInstance(forem.homePageUrl)
        }
    }

    private fun getWebViewFragment(): WebViewFragment? {
        return this.childFragmentManager.findFragmentById(
            R.id.web_view_fragment
        ) as WebViewFragment?
    }

    private fun validateForemLocally(foremCollection: ForemCollection): Forem? {
        val url = URL(deepLinkUrl)
        return foremCollection.foremMap[url.host]
    }

    private fun openOnboardingActivity() {
        sendDataToFragmentResultListener(destinationScreen = DestinationScreen.ONBOARDING)
    }

    fun refresh() {
        getWebViewFragment()?.refresh()
    }

    fun onBackPressedFragment() {
        getWebViewFragment()?.onBackPressedFragment()
    }

    fun onForemNameClicked() {
        sendDataToFragmentResultListener(destinationScreen = DestinationScreen.EXPLORE)
    }

    fun onWebViewBackClicked() {
        getWebViewFragment()?.navigateBack()
    }

    fun onWebViewForwardClicked() {
        getWebViewFragment()?.navigateForward()
    }

    private fun sendDataToFragmentResultListener(
        destinationScreen: DestinationScreen? = DestinationScreen.UNSPECIFIED_SCREEN,
        webViewNavigation: WebViewNavigation? = WebViewNavigation.UNSPECIFIED_NAVIGATION
    ) {
        val homeFragmentResult = HomeFragmentResult.newBuilder()
            .setDestinationScreen(destinationScreen)
            .setWebViewNavigation(webViewNavigation)
            .build()

        val bundle = Bundle()
        bundle.putProto(resultListenerBundleKey, homeFragmentResult)
        setFragmentResult(resultListenerKey, bundle)
    }
}
