package com.forem.android.presentation.preview

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
import com.forem.android.app.model.PreviewFragmentResult
import com.forem.android.databinding.PreviewFragmentBinding
import com.forem.android.presentation.home.RouteToHome
import com.forem.android.utility.getProto
import com.forem.android.utility.putProto
import com.forem.webview.WebViewConstants
import com.forem.webview.WebViewFragment
import dagger.hilt.android.AndroidEntryPoint

private const val WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "PreviewFragment.WebViewFragment.resultListener"

/** Bottom sheet which displays forem preview before adding it. */
@AndroidEntryPoint
open class PreviewFragment : Fragment(), RouteToHome {

    companion object {
        private const val FOREM_ARGUMENT_KEY = "PreviewFragment.forem"
        private const val DEEP_LINK_URL_ARGUMENT_KEY = "PreviewFragment.deepLinkUrl"
        private const val RESULT_LISTENER_KEY = "PreviewFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "PreviewFragment.resultListenerBundle"

        /** Creates a new instance of [PreviewFragment]. */
        fun newInstance(
            forem: Forem,
            deepLinkUrl: String? = null,
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): PreviewFragment {
            val fragment = PreviewFragment()
            val args = Bundle()
            args.putProto(FOREM_ARGUMENT_KEY, forem)
            args.putString(DEEP_LINK_URL_ARGUMENT_KEY, deepLinkUrl)
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    private lateinit var binding: PreviewFragmentBinding
    private lateinit var viewModel: PreviewViewModel

    private var deepLinkUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        val forem = args.getProto(FOREM_ARGUMENT_KEY, Forem.getDefaultInstance())
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        deepLinkUrl = args.getString(DEEP_LINK_URL_ARGUMENT_KEY)

        binding = PreviewFragmentBinding.inflate(
            inflater,
            container,
            /* attachToRoot= */ false
        )

        viewModel = ViewModelProvider(this).get(PreviewViewModel::class.java)
        viewModel.forem.set(forem)
        viewModel.routeToHome = this

        binding.main = this
        binding.viewModel = viewModel

        addCustomAccessibilityActions()

        loadOrUpdateFragment(forem)

        return binding.root
    }

    private fun addCustomAccessibilityActions() {
        ViewCompat.addAccessibilityAction(
            binding.foremPreviewRootLayout,
            this.getString(R.string.add_to_list)
        ) { _, _ ->
            viewModel.onAddToListButtonClicked()
            true
        }
    }

    private fun loadOrUpdateFragment(forem: Forem) {
        val url: String = if (deepLinkUrl == null || deepLinkUrl!!.isEmpty()) {
            forem.homePageUrl
        } else {
            deepLinkUrl!!
        }
        if (getWebViewFragment() == null) {
            val needsForemMetaData = viewModel.forem.get()?.logo.isNullOrEmpty()
            // Disable button if waiting for forem meta data.
            viewModel.isAddButtonEnabled.set(!needsForemMetaData)
            if (needsForemMetaData) {
                // Set result listener only if we need forem meta data.
                childFragmentManager.setFragmentResultListener(
                    WebViewConstants.FOREM_META_DATA_RESULT_LISTENER_KEY,
                    /* lifecycleOwner= */ this
                ) { _, bundle ->
                    val foremUrl = bundle.getString(WebViewConstants.FOREM_META_DATA_URL_KEY)
                    val title = bundle.getString(WebViewConstants.FOREM_META_DATA_TITLE_KEY)
                    val logo = bundle.getString(WebViewConstants.FOREM_META_DATA_LOGO_KEY)

                    val completeForem = Forem.newBuilder()
                        .setHomePageUrl(foremUrl)
                        .setName(title)
                        .setLogo(logo)
                        .build()
                    viewModel.isAddButtonEnabled.set(true)
                    viewModel.forem.set(completeForem)
                }
            }
            childFragmentManager.beginTransaction().add(
                R.id.web_view_fragment,
                WebViewFragment.newInstance(
                    url,
                    // TODO: Mostly this is not required as it is not getting used.
                    WEB_VIEW_FRAGMENT_RESULT_LISTENER_KEY,
                    needsForemMetaData = needsForemMetaData
                )
            ).commitNow()
        } else {
            getWebViewFragment()?.updateForemInstance(forem.homePageUrl)
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

    fun onBackClicked() {
        sendDataToFragmentResultListener(DestinationScreen.CLOSE_CURRENT)
    }

    override fun routeToHome() {
        sendDataToFragmentResultListener(DestinationScreen.HOME)
    }

    private fun sendDataToFragmentResultListener(
        destinationScreen: DestinationScreen? = DestinationScreen.UNSPECIFIED_SCREEN
    ) {
        val previewFragmentResult = PreviewFragmentResult.newBuilder()
            .setDestinationScreen(destinationScreen)
            .build()

        val bundle = Bundle()
        bundle.putProto(resultListenerBundleKey, previewFragmentResult)
        setFragmentResult(resultListenerKey, bundle)
    }
}
