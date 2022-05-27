package com.forem.android.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.forem.android.R
import com.forem.android.app.model.AddForemFragmentResult
import com.forem.android.app.model.DestinationScreen
import com.forem.android.app.model.Forem
import com.forem.android.app.model.OnboardingFragmentResult
import com.forem.android.app.model.PreviewFragmentData
import com.forem.android.app.model.PreviewFragmentResult
import com.forem.android.presentation.addforem.AddForemFragment
import com.forem.android.presentation.home.HomeActivity
import com.forem.android.presentation.preview.PreviewFragment
import com.forem.android.utility.getProto
import dagger.hilt.android.AndroidEntryPoint

private const val ADD_FOREM_FRAGMENT_RESULT_LISTENER_KEY =
    "OnboardingActivity.AddForemFragment.resultListener"
private const val ADD_FOREM_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "OnboardingActivity.AddForemFragment.resultListenerBundle"

private const val ONBOARDING_FRAGMENT_RESULT_LISTENER_KEY =
    "OnboardingActivity.OnboardingFragment.resultListener"
private const val ONBOARDING_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "OnboardingActivity.OnboardingFragment.resultListenerBundle"

private const val PREVIEW_FRAGMENT_RESULT_LISTENER_KEY =
    "OnboardingActivity.PreviewFragment.resultListener"
private const val PREVIEW_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY =
    "OnboardingActivity.PreviewFragment.resultListenerBundle"

/**
 * Activity which displays [OnboardingFragment] along with its child fragments i.e.
 * [AddForemFragment] and [PreviewFragment].
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    companion object {
        /**
         * Start [OnboardingActivity] by creating a new instance using intents.
         *
         * @param context Context of parent/source activity.
         *
         * @return Returns Intent for [OnboardingActivity]
         */
        fun newInstance(context: Context): Intent {
            return Intent(context, OnboardingActivity::class.java)
        }
    }

    private lateinit var addForemFragment: AddForemFragment
    private lateinit var previewFragment: PreviewFragment
    private lateinit var onboardingFragment: OnboardingFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_activity)

        if (savedInstanceState == null) {
            replaceFragment(createOnboardingFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO(#176): Optimise these result listeners to attach as-per-need basis only.
        createOnboardingFragmentResultListener()
        createAddForemFragmentResultListener()
        createPreviewFragmentResultListener()
    }

    override fun onBackPressed() {
        if (::onboardingFragment.isInitialized && onboardingFragment.isVisible) {
            finish()
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

    private fun createOnboardingFragment(): OnboardingFragment {
        onboardingFragment = OnboardingFragment.newInstance(
            ONBOARDING_FRAGMENT_RESULT_LISTENER_KEY,
            ONBOARDING_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY
        )
        return onboardingFragment
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

    private fun createOnboardingFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(
            ONBOARDING_FRAGMENT_RESULT_LISTENER_KEY,
            /* lifecycleOwner= */ this
        ) { _, bundle ->
            val onboardingFragmentResult = bundle.getProto(
                ONBOARDING_FRAGMENT_RESULT_LISTENER_BUNDLE_KEY,
                OnboardingFragmentResult.getDefaultInstance()
            )

            val previewFragmentData = onboardingFragmentResult.previewFragmentData

            val destinationScreen = onboardingFragmentResult.destinationScreen
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
            DestinationScreen.PASSPORT -> {}
            DestinationScreen.ADD_FOREM -> {
                replaceFragment(createAddForemFragment())
            }
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
                startActivity(HomeActivity.newInstance(this))
                finish()
            }
            DestinationScreen.CLOSE_CURRENT -> {
                onBackPressed()
            }
            DestinationScreen.UNRECOGNIZED -> {
                // TODO(#176): Log this error
            }
        }
    }
}
