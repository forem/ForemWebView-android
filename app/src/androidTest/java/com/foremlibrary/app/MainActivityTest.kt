package com.foremlibrary.app

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context: Context = getInstrumentation().targetContext

    @Test
    fun testMainActivity_backButton_isDisabledByDefault() {
        launch<MainActivity>(createMainActivityIntent("")).use {
            onView(withId(R.id.back_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_forwardButton_isDisabledByDefault() {
        launch<MainActivity>(createMainActivityIntent("")).use {
            onView(withId(R.id.forward_image_view)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testMainActivity_forwardButton_loadWebView() {
        launch<MainActivity>(createMainActivityIntent(getForemDevLocalUrl())).use {
            onView(isRoot()).perform(waitFor(20000))
        }
    }

    private fun createMainActivityIntent(foremUrl: String): Intent {
        return MainActivity.newInstance(context = context, url = foremUrl)
    }

    private fun getForemDevLocalUrl(): String {
        // return File("src/androidTest/assets/forem.dev.html").absolutePath
        return "file:///android_asset/forem.dev.html"
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
