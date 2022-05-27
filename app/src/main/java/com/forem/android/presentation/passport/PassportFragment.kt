package com.forem.android.presentation.passport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.forem.android.R
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.PassportFragmentResult
import com.forem.android.app.model.WebViewFragmentResult
import com.forem.android.app.model.WebViewNavigation
import com.forem.android.databinding.PassportFragmentBinding
import com.forem.android.utility.putProto
import com.forem.webview.WebViewConstants
import com.forem.webview.WebViewFragment
import dagger.hilt.android.AndroidEntryPoint

private const val WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "PassportFragment.WebViewFragment.resultListener"

/** Fragment which displays forem passport. */
@AndroidEntryPoint
open class PassportFragment : Fragment() {

    companion object {
        private const val RESULT_LISTENER_KEY = "PassportFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "PassportFragment.resultListenerBundle"

        /** Creates a new instance of [PassportFragment]. */
        fun newInstance(
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): PassportFragment {
            val fragment = PassportFragment()
            val args = Bundle()
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    private lateinit var binding: PassportFragmentBinding
    private lateinit var viewModel: PassportViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        binding = PassportFragmentBinding.inflate(
            inflater,
            container,
            /* attachToRoot= */ false
        )

        viewModel = ViewModelProvider(this).get(PassportViewModel::class.java)

        binding.main = this
        binding.viewModel = viewModel

        loadPassportWebViewFragment()

        return binding.root
    }

    private fun loadPassportWebViewFragment() {
        if (getWebViewFragment() == null) {
            childFragmentManager.setFragmentResultListener(
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                /* lifecycleOwner= */ this
            ) { _, bundle ->

                val canGoBack = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_BACK_KEY)
                val canGoForward = bundle.getBoolean(WebViewConstants.WEB_VIEW_CAN_GO_FORWARD_KEY)
                val homeReached = bundle.getBoolean(WebViewConstants.WEB_VIEW_HOME_REACHED_KEY)

                val webViewNavigation = if (homeReached)
                    WebViewNavigation.HOME_REACHED
                else
                    WebViewNavigation.UNSPECIFIED_NAVIGATION

                val webViewFragmentResult = WebViewFragmentResult.newBuilder()
                    .setWebViewNavigation(webViewNavigation)
                    .setCanGoBack(canGoBack)
                    .setCanGoForward(canGoForward)
                    .build()

                sendDataToFragmentResultListener(webViewNavigation = webViewFragmentResult.webViewNavigation)
            }

            val webViewFragment = WebViewFragment.newInstance(
                WebViewConstants.PASSPORT_URL,
                WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                needsForemMetaData = false
            )
            childFragmentManager.beginTransaction().add(
                R.id.web_view_fragment,
                webViewFragment
            ).commitNow()
        }
    }

    private fun getWebViewFragment(): WebViewFragment? {
        return childFragmentManager.findFragmentById(
            R.id.web_view_fragment
        ) as WebViewFragment?
    }

    fun refresh() {
        getWebViewFragment()?.refresh()
    }

    fun onBackPressedFragment() {
        getWebViewFragment()?.onBackPressedFragment()
    }

    fun onBackClicked() {
        sendDataToFragmentResultListener(DestinationScreen.CLOSE_CURRENT)
    }

    private fun sendDataToFragmentResultListener(
        destinationScreen: DestinationScreen? = DestinationScreen.UNSPECIFIED_SCREEN,
        webViewNavigation: WebViewNavigation? = WebViewNavigation.UNSPECIFIED_NAVIGATION
    ) {
        val passportFragmentResult = PassportFragmentResult.newBuilder()
            .setDestinationScreen(destinationScreen)
            .setWebViewNavigation(webViewNavigation)
            .build()

        val bundle = Bundle()
        bundle.putProto(resultListenerBundleKey, passportFragmentResult)
        setFragmentResult(resultListenerKey, bundle)
    }
}
