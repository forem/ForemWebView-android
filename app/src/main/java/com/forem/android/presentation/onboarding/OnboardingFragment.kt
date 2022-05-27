package com.forem.android.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.forem.android.R
import com.forem.android.app.model.AddForemFragmentResult
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.Forem
import com.forem.android.app.model.PreviewFragmentData
import com.forem.android.databinding.OnboardingFragmentBinding
import com.forem.android.presentation.addforem.RouteToAddForem
import com.forem.android.presentation.preview.RouteToPreview
import com.forem.android.utility.Resource
import com.forem.android.utility.putProto
import dagger.hilt.android.AndroidEntryPoint

/** Displays forem list on onboarding screen via API call */
@AndroidEntryPoint
class OnboardingFragment : Fragment(), RouteToAddForem, RouteToPreview {

    companion object {
        private const val RESULT_LISTENER_KEY = "OnboardingFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "OnboardingFragment.resultListenerBundle"

        /** Creates a new instance of [OnboardingFragment]. */
        fun newInstance(
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    private lateinit var onboardingAdapter: OnboardingAdapter
    private lateinit var binding: OnboardingFragmentBinding
    private lateinit var viewModel: OnboardingFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        binding = OnboardingFragmentBinding.inflate(inflater, container, /* attachToRoot= */ false)
        viewModel = ViewModelProvider(this).get(OnboardingFragmentViewModel::class.java)
        binding.viewModel = viewModel
        setupRecyclerView()
        setupObservers()
        return binding.root
    }

    private fun setupObservers() {
        viewModel.featuredForems.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.data != null && !it.data.foremMap.isNullOrEmpty()) {
                        viewModel.loadingViewVisibility.set(false)
                    }

                    it.data?.foremMap?.forEach { (_, forem) ->
                        val onboardingForemItemViewModel =
                            OnboardingForemItemViewModel(forem, routeToPreview = this)
                        if (::onboardingAdapter.isInitialized) {
                            onboardingAdapter.addItem(onboardingForemItemViewModel)
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    // TODO(#175): Show proper UI for errors and log this error.
                    viewModel.loadingViewVisibility.set(false)
                    Toast.makeText(
                        this.requireContext(),
                        getString(R.string.network_not_reachable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Resource.Status.LOADING -> {
                    viewModel.loadingViewVisibility.set(true)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val onboardingHeaderItemViewModel =
            OnboardingHeaderItemViewModel(routeToAddForem = this)
        onboardingAdapter = OnboardingAdapter()
        onboardingAdapter.addItem(onboardingHeaderItemViewModel)

        val linearLayoutManager = LinearLayoutManager(this.requireContext())
        binding.foremRecyclerView.apply {
            layoutManager = linearLayoutManager
            adapter = onboardingAdapter
        }
    }

    override fun routeToAddForem() {
        sendDataToFragmentResultListener(DestinationScreen.ADD_FOREM)
    }

    override fun routeToPreview(forem: Forem, deepLinkUrl: String?) {
        val previewFragmentDataBuilder = PreviewFragmentData.newBuilder()
        previewFragmentDataBuilder.forem = forem
        if (!deepLinkUrl.isNullOrEmpty()) {
            previewFragmentDataBuilder.deepLinkUrl = deepLinkUrl
        }
        sendDataToFragmentResultListener(
            DestinationScreen.PREVIEW,
            previewFragmentDataBuilder.build()
        )
    }

    private fun sendDataToFragmentResultListener(
        destinationScreen: DestinationScreen? = DestinationScreen.UNSPECIFIED_SCREEN,
        previewFragmentData: PreviewFragmentData? = PreviewFragmentData.getDefaultInstance()
    ) {
        val addForemFragmentResult = AddForemFragmentResult.newBuilder()
            .setDestinationScreen(destinationScreen)
            .setPreviewFragmentData(previewFragmentData)
            .build()

        val bundle = Bundle()
        bundle.putProto(resultListenerBundleKey, addForemFragmentResult)
        setFragmentResult(resultListenerKey, bundle)
    }
}
