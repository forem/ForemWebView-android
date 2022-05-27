package com.forem.android.presentation.splash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.forem.android.R
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import com.forem.android.app.model.PreviewFragmentResult
import com.forem.android.data.repository.MyForemRepository
import com.forem.android.databinding.SplashActivityBinding
import com.forem.android.presentation.home.HomeActivity
import com.forem.android.presentation.onboarding.OnboardingActivity
import com.forem.android.presentation.preview.PreviewFragment
import com.forem.android.utility.DataConvertors
import com.forem.android.utility.Resource
import com.forem.android.utility.getProto
import com.forem.webview.UrlChecks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

private const val PREVIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "SplashActivity.PreviewFragment.resultListener"
private const val PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "SplashActivity.PreviewFragment.resultListenerBundle"

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var myForemRepository: MyForemRepository

    private var deepLinkUrl: String? = null

    private lateinit var binding: SplashActivityBinding

    private lateinit var previewFragment: PreviewFragment

    /**
     * LOGIC
     *
     * Get deep link url if exists
     * Get all local forems
     *  -> If deep link url DOES NOT exists
     *      -> Route to either OnboardingActivity or HomeActivity
     *  -> Else deep link url exists
     *      -> If local forems are EMPTY
     *          -> Validate url remotely
     *      -> Else local forems are available
     *          -> Validate url locally
     *
     * Validate url remotely
     *  -> If INVALID
     *      -> Show alert
     *  -> else
     *      -> Show preview
     *
     * Validate url locally
     *  -> If INVALID
     *      -> Validate url remotely
     *  -> Else
     *      -> Update current forem
     *
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.splash_activity)

        // Cases for deepLinkUrl checking:
        // 1. Valid current selected forem url
        // 2. Valid current unselected forem url
        // 3. Valid not-added forem url
        // 4. Invalid url for valid forem
        // 5. Invalid url for invalid forem

        // For different cases to test check this link:
        // https://gist.github.com/rt4914/8ad7f47715cc652fbaf32baccdadfaf6

        deepLinkUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!deepLinkUrl.isNullOrEmpty()) {
            if (UrlChecks.checkUrlIsCorrect(deepLinkUrl!!)) {
                deepLinkUrl = UrlChecks.checkAndAppendHttpsToUrl(deepLinkUrl!!)
                deepLinkUrl = Uri.parse(deepLinkUrl).toString()
                deepLinkUrl = URL(deepLinkUrl).toURI().toString()
            } else {
                deepLinkUrl = null
            }
        }

        getAllLocalForems()
    }

    override fun onBackPressed() {
        finish()
    }

    private fun getAllLocalForems() {
        myForemRepository.getMyForems().observe(
            this
        ) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (deepLinkUrl == null) {
                        if (it.data == null || it.data.foremCount == 0) {
                            openOnboardingActivity()
                        } else {
                            openHomeActivity()
                        }
                    } else {
                        if (it.data == null || it.data.foremCount == 0) {
                            // Validate URL remotely
                            validateForemRemotely()
                        } else {
                            // Validate URL locally
                            val localForem: Forem? = validateForemLocally(it.data)
                            if (localForem == null) {
                                // Validate URL remotely
                                validateForemRemotely()
                            } else {
                                updateCurrentForem(localForem)
                            }
                        }
                    }
                }
                Resource.Status.ERROR -> {
                    showInvalidUrlAlert()
                }
                Resource.Status.LOADING -> {
                    // No loading screen required
                }
            }
        }
    }

    private fun updateCurrentForem(forem: Forem) {
        lifecycleScope.launch(Dispatchers.IO) {
            myForemRepository.updateCurrentForem(forem)
            withContext(Dispatchers.Main) {
                openHomeActivity()
            }
        }
    }

    private fun validateForemLocally(foremCollection: ForemCollection): Forem? {
        val url = URL(deepLinkUrl)
        return foremCollection.foremMap[url.host]
    }

    private fun validateForemRemotely() {
        val uri = Uri.parse(deepLinkUrl)
        if (uri == null || uri.host == null) {
            showInvalidUrlAlert()
            return
        }
        val domain: String = uri.host!!

        showForemPreview(DataConvertors.createForemFromUrl(domain))
    }

    private fun showInvalidUrlAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.invalid_url)
        builder.setMessage(R.string.invalid_url_message)
        builder.setPositiveButton(R.string.exit) { _, _ ->
            finish()
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun showForemPreview(forem: Forem) {
        binding.fragmentPlaceholder.visibility = View.VISIBLE
        addFragment(createPreviewFragment(forem, deepLinkUrl))
    }

    private fun addFragment(fragment: Fragment) {
        val tag: String = fragment::class.java.name
        val manager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = manager.beginTransaction()
        if (manager.findFragmentByTag(tag) == null) {
            fragmentTransaction.add(R.id.fragment_placeholder, fragment, tag)
            fragmentTransaction.addToBackStack(tag)
            fragmentTransaction.commit()
        }
    }

    private fun removeFragment(fragment: Fragment) {
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.remove(fragment)
        fragmentTransaction.commit()
        supportFragmentManager.popBackStack()
    }

    private fun createPreviewFragment(forem: Forem, deepLinkUrl: String?): PreviewFragment {
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

        previewFragment = PreviewFragment.newInstance(
            forem,
            deepLinkUrl,
            PREVIEW_FRAGMENT_RESULT_LISTENER_KEY,
            PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return previewFragment
    }

    private fun openOnboardingActivity() {
        val intent = OnboardingActivity.newInstance(this)
        startActivity(intent)
        finish()
    }

    private fun openHomeActivity() {
        val intent = HomeActivity.newInstance(this, deepLinkUrl)
        startActivity(intent)
        finish()
    }

    private fun routeToDestinationScreen(
        destinationScreen: DestinationScreen?
    ) {
        if (destinationScreen == null) {
            return
        }
        when (destinationScreen) {
            DestinationScreen.UNSPECIFIED_SCREEN -> {}
            DestinationScreen.EXPLORE -> {}
            DestinationScreen.ONBOARDING -> {
                val intent = OnboardingActivity.newInstance(context = this)
                startActivity(intent)
                finish()
            }
            DestinationScreen.PASSPORT -> {}
            DestinationScreen.ADD_FOREM -> {}
            DestinationScreen.HOME -> {
                if (::previewFragment.isInitialized) {
                    removeFragment(previewFragment)
                    binding.fragmentPlaceholder.visibility = View.VISIBLE
                }
                finish()
            }
            DestinationScreen.PREVIEW -> {}
            DestinationScreen.CLOSE_CURRENT -> {
                if (::previewFragment.isInitialized) {
                    removeFragment(previewFragment)
                    binding.fragmentPlaceholder.visibility = View.VISIBLE
                }
                finish()
            }
            DestinationScreen.UNRECOGNIZED -> {
                // TODO(#176): Log this error
            }
        }
    }
}
