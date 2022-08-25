package com.foremlibrary.app

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView

private const val DEV_TO = "https://dev.to"

@RunWith(AndroidJUnit4::class)
class MainActivityUnitTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val activityTestRule = ActivityTestRule(
        MainActivity::class.java,
        /* initialTouchMode= */ true,
        /* launchActivity= */ false
    )

    @Test
    fun testMainActivity_backImageView_isDisabledByDefault() {
        activityTestRule.launchActivity(createMainActivityIntent(""))
        onView(withId(R.id.back_image_view)).check(matches(not(isEnabled())))
    }

    @Test
    fun testMainActivity_forwardImageView_isDisabledByDefault() {
        activityTestRule.launchActivity(createMainActivityIntent(""))
        onView(withId(R.id.forward_image_view)).check(matches(not(isEnabled())))
    }

    @Test
    fun testMainActivity_loadDevLocal_urlIsLoaded() {
        activityTestRule.launchActivity(createMainActivityIntent(DEV_TO))
        val webView: WebView = activityTestRule.activity.findViewById(R.id.web_view)
        val shadowWebView: ShadowWebView = shadowOf(webView)
        assertThat(shadowWebView.lastLoadedUrl).contains(DEV_TO)
    }

    private fun createMainActivityIntent(foremUrl: String): Intent {
        return MainActivity.newInstance(context = context, url = foremUrl)
    }
}
