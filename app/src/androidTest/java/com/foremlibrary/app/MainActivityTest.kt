package com.foremlibrary.app

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.forem.webview.video.VideoPlayerActivity
import com.foremlibrary.app.testing.EspressoIdlingResource
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeoutException

private const val DEV_LOCAL_1 = "file:///android_asset/forem.dev.html"

private const val ACCOUNTS_FOREM_URL = "https://account.forem.com"
private const val DEV_TO = "https://dev.to"
private const val MMA_LIFE = "https://www.thismmalife.com/"
private const val DEV_TO_CONTACT = "https://dev.to/contact"

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val context: Context = getInstrumentation().targetContext

    // TODO: Pending Test Cases
    //  - AudioPlayer works -> We need UiAutomator for this test. It cannot be tested using Espresso.
    //  - Image Upload does work -> We need UiAutomator for this test as sign-in process is not easy.
    //  - Custom Tab Intent -> We need UiAutomator for this test.

    @Before
    fun setup() {
        Intents.init()
        EspressoIdlingResource.increment()
    }

    @After
    fun tearDown() {
        Intents.release()
        EspressoIdlingResource.decrement()
    }

    @Test
    fun testMainActivity_backImageView_isDisabledByDefault() {
        launch<MainActivity>(createMainActivityIntent("")).use {
            onView(withId(R.id.back_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_forwardImageView_isDisabledByDefault() {
        launch<MainActivity>(createMainActivityIntent("")).use {
            onView(withId(R.id.forward_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_webViewIsDisplayed() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community")))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_backImageViewIsEnabled() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community - Page 2")))

            onView(isRoot())
                .perform(waitForView(R.id.back_image_view, 5000))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_forwardImageViewIsDisabled() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community - Page 2")))

            onView(withId(R.id.forward_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_goToPreviousPage_backImageViewIsDisabled() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "go-to-previous-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community")))

            onView(withId(R.id.back_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_goToPreviousPage_forwardImageViewIsEnabled() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "go-to-previous-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community")))

            onView(withId(R.id.forward_image_view)).check(matches(isEnabled()))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_clickBackImageView_page1IsDisplayed() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onView(withId(R.id.back_image_view)).perform(click())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community")))
        }
    }

    @Test
    fun testMainActivity_loadDevLocal_goToNextPage_goToPreviousPage_clickForwardImageView_page2IsDisplayed() {
        launch<MainActivity>(createMainActivityIntent(DEV_LOCAL_1)).use {
            onWebView()
                .withElement(findElement(Locator.ID, "go-to-next-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "go-to-previous-page"))
                .perform(webClick())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community")))

            onView(withId(R.id.forward_image_view)).perform(click())

            onWebView()
                .withElement(findElement(Locator.ID, "forem-title"))
                .check(webMatches(getText(), containsString("DEV Community - Page 2")))
        }
    }

    @Test
    fun testMainActivity_loadAccountsForemRemote_homePageIsDisplayed() {
        launchActivity<MainActivity>(createMainActivityIntent(ACCOUNTS_FOREM_URL))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "btn-primary-cta"))
            .check(webMatches(getText(), containsString("Get Started with Forem")))

        onView(withId(R.id.web_view)).check(matches(isDisplayed()))
        onView(withId(R.id.oauth_web_view_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testMainActivity_loadMMALifeRemote_createAccount_signUpWithGoogle_urlOpensInOtherWebView() {
        launchActivity<MainActivity>(createMainActivityIntent(MMA_LIFE))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "c-cta--branded"))
            .check(webMatches(getText(), containsString("Create account")))
            .perform(webClick())

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "crayons-btn--brand-google_oauth2"))
            .check(webMatches(getText(), containsString("Sign up with Google")))
            .perform(webClick())

        onView(isRoot())
            .perform(waitForView(R.id.oauth_web_view_container, 5000))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMainActivity_loadMMALifeRemote_createAccount_signUpWithForem_urlOpensInSameWebView() {
        launchActivity<MainActivity>(createMainActivityIntent(MMA_LIFE))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "c-cta--branded"))
            .check(webMatches(getText(), containsString("Create account")))
            .perform(webClick())

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "crayons-btn--brand-forem"))
            .check(webMatches(getText(), containsString("Sign up with Forem")))
            .perform(webClick())

        onView(withId(R.id.web_view)).check(matches(isDisplayed()))
        onView(withId(R.id.oauth_web_view_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testMainActivity_loadDevToRemote_createAccount_signUpWithGithub_urlOpensInSameWebView() {
        launchActivity<MainActivity>(createMainActivityIntent(DEV_TO))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "c-cta--branded"))
            .check(webMatches(getText(), containsString("Create account")))
            .perform(webClick())

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "crayons-btn--brand-github"))
            .check(webMatches(getText(), containsString("Sign up with GitHub")))
            .perform(webClick())

        onView(withId(R.id.web_view)).check(matches(isDisplayed()))
        onView(withId(R.id.oauth_web_view_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testMainActivity_loadMMALifeRemote_createAccount_signUpWithTwitter_urlOpensInSameWebView() {
        launchActivity<MainActivity>(createMainActivityIntent(MMA_LIFE))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "c-cta--branded"))
            .check(webMatches(getText(), containsString("Create account")))
            .perform(webClick())

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "crayons-btn--brand-twitter"))
            .check(webMatches(getText(), containsString("Sign up with Twitter")))
            .perform(webClick())

        onView(withId(R.id.web_view)).check(matches(isDisplayed()))
        onView(withId(R.id.oauth_web_view_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testMainActivity_loadDevToRemote_clickOnEmail_opensEmailIntent() {
        launchActivity<MainActivity>(createMainActivityIntent(DEV_TO_CONTACT))

        onWebView()
            .withElement(findElement(Locator.LINK_TEXT, "yo@dev.to"))
            .perform(webClick())

        intended(
            allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(Intent.EXTRA_TITLE, "Send email with…")
            )
        )
    }

    @Test
    fun testMainActivity_loadDevArticle_clickOnMoreOptions_opensShareIntent() {
        launchActivity<MainActivity>(createMainActivityIntent("https://dev.to/devteam/for-empowering-community-2k6h"))

        onWebView()
            .withElement(findElement(Locator.ID, "article-show-more-button"))
            .perform(webClick())

        intended(
            allOf(
                hasAction(Intent.ACTION_CHOOSER)
            )
        )
    }

    @Test
    fun testMainActivity_loadDevVideoArticle_playVideo_opensVideoActivity() {
        launchActivity<MainActivity>(createMainActivityIntent("https://dev.to/ben/why-forem-is-special-with-ben-halpern-1d61"))

        onWebView()
            .withElement(findElement(Locator.ID, "video-player-407788"))
            .perform(webClick())

        intended(hasComponent(VideoPlayerActivity::class.java.name))
        intended(
            hasExtra(
                VideoPlayerActivity.VIDEO_URL_INTENT_EXTRA,
                "https://dw71fyauz7yz9.cloudfront.net/video-upload__a8eaf9049e79b4def2008c6c4b9bff09/video-upload__a8eaf9049e79b4def2008c6c4b9bff09.m3u8"
            )
        )
        intended(hasExtra(VideoPlayerActivity.VIDEO_TIME_INTENT_EXTRA, "0"))
    }

    @Test
    fun testMainActivity_loadDevTo_foremNameIsCorrectlyDisplayed() {
        launchActivity<MainActivity>(createMainActivityIntent(DEV_TO))

        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "c-cta--branded"))
            .check(webMatches(getText(), containsString("Create account")))

        onView(withId(R.id.forem_name_text_view))
            .check(matches(withText(containsString("DEV"))))
    }

    private fun createMainActivityIntent(foremUrl: String): Intent {
        return MainActivity.newInstance(context = context, url = foremUrl)
    }

    /**
     * This ViewAction tells espresso to wait till a certain view is found in the view hierarchy.
     * Reference: https://www.repeato.app/espresso-wait-for-view/
     * @param viewId The id of the view to wait for.
     * @param timeout The maximum time which espresso will wait for the view to show up (in milliseconds)
     */
    private fun waitForView(viewId: Int, timeout: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "wait for a specific view with id $viewId; during $timeout millis."
            }

            override fun perform(uiController: UiController, rootView: View) {
                uiController.loopMainThreadUntilIdle()
                val startTime = System.currentTimeMillis()
                val endTime = startTime + timeout
                val viewMatcher = withId(viewId)

                do {
                    // Iterate through all views on the screen and see if the view we are looking for is there already
                    for (child in TreeIterables.breadthFirstViewTraversal(rootView)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return
                        }
                    }
                    // Loops the main thread for a specified period of time.
                    // Control may not return immediately, instead it'll return after the provided delay has passed and the queue is in an idle state again.
                    uiController.loopMainThreadForAtLeast(100)
                } while (System.currentTimeMillis() < endTime) // in case of a timeout we throw an exception -> test fails
                throw PerformException.Builder()
                    .withCause(TimeoutException())
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(rootView))
                    .build()
            }
        }
    }
}
