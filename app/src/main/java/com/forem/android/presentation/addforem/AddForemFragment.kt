package com.forem.android.presentation.addforem

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.forem.android.R
import com.forem.android.app.model.AddForemFragmentResult
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.PreviewFragmentData
import com.forem.android.databinding.AddForemFragmentBinding
import com.forem.android.utility.DataConvertors
import com.forem.android.utility.Resource
import com.forem.android.utility.putProto
import com.forem.webview.UrlChecks
import com.forem.webview.UrlChecks.isValidUrl
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL

/** Bottom sheet which helps in adding private or unlisted forem. */
@AndroidEntryPoint
open class AddForemFragment : Fragment(), NextButtonListener {

    companion object {
        private const val RESULT_LISTENER_KEY = "AddForemFragment.resultListener"
        private const val RESULT_LISTENER_BUNDLE_KEY = "AddForemFragment.resultListenerBundle"

        /** Creates a new instance of [AddForemFragment]. */
        fun newInstance(
            resultListenerKey: String,
            resultListenerBundleKey: String
        ): AddForemFragment {
            val fragment = AddForemFragment()
            val args = Bundle()
            args.putString(RESULT_LISTENER_KEY, resultListenerKey)
            args.putString(RESULT_LISTENER_BUNDLE_KEY, resultListenerBundleKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var resultListenerKey: String
    private lateinit var resultListenerBundleKey: String

    private lateinit var binding: AddForemFragmentBinding
    private lateinit var viewModel: AddForemViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        resultListenerKey = args.getString(RESULT_LISTENER_KEY)!!
        resultListenerBundleKey = args.getString(RESULT_LISTENER_BUNDLE_KEY)!!

        binding = AddForemFragmentBinding.inflate(
            inflater,
            container,
            /* attachToRoot= */ false
        )

        viewModel = ViewModelProvider(this).get(AddForemViewModel::class.java)
        viewModel.nextButtonListener = this

        binding.main = this
        binding.viewModel = viewModel

        binding.addForemTextInputEditText.doOnTextChanged { foremDomain, _, _, _ ->
            foremDomain?.let {
                viewModel.userInputForem.set(foremDomain.toString())
                if (foremDomain.isEmpty()) {
                    viewModel.addForemErrorMsg.set(this.getString(R.string.error_missing_domain))
                } else {
                    viewModel.addForemErrorMsg.set("")
                }
            }
        }

        binding.addForemTextInputEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER))
            ) {
                onNextButtonClicked()
            }
            false
        }
        return binding.root
    }

    /** This function checks if the domain name entered by user already exists locally or not.*/
    private fun validateLocalForemObserver(domain: String) {
        val newDomain: String = Uri.parse(domain).toString()
        viewModel.myForemCollection.observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    val foremMap = it.data?.foremMap
                    val url = UrlChecks.checkAndAppendHttpsToUrl(newDomain)
                    try {
                        val domainUrl = URL(url)
                        if (!foremMap.isNullOrEmpty() && foremMap.containsKey(domainUrl.host)) {
                            foremDomainAlreadyExistsLocally()
                            return@observe
                        }
                    } catch (e: Exception) {
                        Log.e("AddForemFragment", "validateLocalForemObserver: error", e)
                    }
                    validateForemOnRemote(newDomain)
                    return@observe
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

    private fun foremDomainAlreadyExistsLocally() {
        viewModel.addForemErrorMsg.set(getString(R.string.forem_already_available_in_list))
    }

    private fun validateForemOnRemote(domain: String) {
        viewModel.loadingViewVisibility.set(false)
        hideKeyboard()
        val previewFragmentData = PreviewFragmentData.newBuilder()
            .setForem(DataConvertors.createForemFromUrl(domain))
            .build()
        sendDataToFragmentResultListener(
            DestinationScreen.PREVIEW,
            previewFragmentData
        )
    }

    override fun onNextButtonClicked() {
        val foremDomain = viewModel.userInputForem.get()!!
        when {
            foremDomain.isEmpty() -> {
                viewModel.addForemErrorMsg.set(this.getString(R.string.error_missing_domain))
            }
            foremDomain.isValidUrl() -> {
                val newDomain = UrlChecks.checkAndAppendHttpsToUrl(foremDomain)
                validateLocalForemObserver(newDomain)
            }
            else -> {
                viewModel.addForemErrorMsg.set(this.getString(R.string.error_invalid_url))
            }
        }
    }

    private fun hideKeyboard() {
        val activity = this.activity
        this.context?.apply {
            if (activity != null) {
                val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(activity.window.currentFocus?.windowToken, 0)
            }
        }
    }

    fun onBackClicked() {
        sendDataToFragmentResultListener(DestinationScreen.CLOSE_CURRENT)
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
