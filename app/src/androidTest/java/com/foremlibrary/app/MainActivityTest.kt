package com.foremlibrary.app

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

private const val DEV_LOCAL_1 = "file:///android_asset/forem.dev.html"
private const val DEV_LOCAL_2 = "file:///android_asset/forem.dev_2.html"

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val context: Context = getInstrumentation().targetContext

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

            onView(withId(R.id.back_image_view)).check(matches(isEnabled()))
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

    private fun createMainActivityIntent(foremUrl: String): Intent {
        return MainActivity.newInstance(context = context, url = foremUrl)
    }

    /**
     * Perform action of waiting for a specific time.
     */
    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait for $millis milliseconds."
            }

            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
}
