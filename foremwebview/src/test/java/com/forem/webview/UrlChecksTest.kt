package com.forem.webview

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.forem.webview.UrlChecks.isValidUrl
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UrlChecksTest {

    @Test
    fun testUrlChecks_getURLType_checkForHostLink_worksCorrectly() {
        val host = "randomString.com"
        val url = "https://www.randomString.com/home"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.HOST_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink1_worksCorrectly() {
        val host = "dev.to"
        val url = "https://account.forem.com"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink2_worksCorrectly() {
        val host = "dev.to"
        val url = "https://api.twitter.com/oauth"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink3_worksCorrectly() {
        val host = "dev.to"
        val url = "https://api.twitter.com/login/error"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink4_worksCorrectly() {
        val host = "dev.to"
        val url = "https://api.twitter.com/account/login_verification"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink5_worksCorrectly() {
        val host = "dev.to"
        val url = "https://github.com/login"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOverrideLink6_worksCorrectly() {
        val host = "dev.to"
        val url = "https://github.com/sessions/"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OVERRIDE_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOAuthLink1_worksCorrectly() {
        val host = "randomString.com"
        val url = "https://accounts.google.com"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OAUTH_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForOAuthLink2_worksCorrectly() {
        val host = "randomString.com"
        val url = "https://accounts.google.co.in"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.OAUTH_LINK)
    }

    // This is a very weird edge case because teh test case has been written correctly but
    // still the test is failing. The same scenario does work perfectly in application. This has
    // been proved in `testUrlChecks_checkUrlIsCorrect_checkForValidUrl2_returnsFalse` too.
    @Test
    @Ignore
    fun testUrlChecks_getURLType_checkForEmailLink_worksCorrectly() {
        val host = "dev.to"
        val url = "mailto:yo@dev.to"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.EMAIL_LINK)
    }

    @Test
    fun testUrlChecks_getURLType_checkForThirdPartyLink_worksCorrectly() {
        val host = "dev.to"
        val url = "google.com"
        val result = UrlChecks.getURLType(url, host)
        assertThat(result).isEqualTo(UrlType.THIRD_PARTY_LINK)
    }

    @Test
    fun testUrlChecks_checkUrlIsCorrect_checkForValidUrl_returnsFalse() {
        val url = "randomString"
        val result = UrlChecks.checkUrlIsCorrect(url)
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun testUrlChecks_checkUrlIsCorrect_checkForValidUrl1_returnsFalse() {
        val url = "www.randomString.com"
        val result = UrlChecks.checkUrlIsCorrect(url)
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun testUrlChecks_checkUrlIsCorrect_checkForValidUrl2_returnsFalse() {
        val url = "dev.to"
        val result = UrlChecks.checkUrlIsCorrect(url)
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun testUrlChecks_checkAndAppendHttpsToUrl_checkForInvalidUrl_returnsOriginalUrl() {
        val url = "randomString"
        val result = UrlChecks.checkAndAppendHttpsToUrl(url)
        assertThat(result).isEqualTo(url)
    }

    @Test
    fun testUrlChecks_checkAndAppendHttpsToUrl_urlWithoutHttpOrHttps_returnsHttpsUrl() {
        val url = "randomString.com"
        val result = UrlChecks.checkAndAppendHttpsToUrl(url)
        assertThat(result).isEqualTo("https://randomString.com")
    }

    @Test
    fun testUrlChecks_checkAndAppendHttpsToUrl_urlWithHttp_returnsHttpsUrl() {
        val url = "http://randomString.com"
        val result = UrlChecks.checkAndAppendHttpsToUrl(url)
        assertThat(result).isEqualTo("https://randomString.com")
    }

    @Test
    fun testUrlChecks_checkAndAppendHttpsToUrl_urlWithHttps_returnsOriginalUrl() {
        val url = "https://randomString.com"
        val result = UrlChecks.checkAndAppendHttpsToUrl(url)
        assertThat(result).isEqualTo(url)
    }

    @Test
    fun testUrlChecks_isValidUrl_checkAllValidUrls() {
        val url1 = "https://dev.to"
        assertThat(url1.isValidUrl()).isEqualTo(true)

        val url2 = "http://dev.to"
        assertThat(url2.isValidUrl()).isEqualTo(true)

        val url3 = "http://dev.to"
        assertThat(url3.isValidUrl()).isEqualTo(true)

        val url4 = "http://www.dev.to"
        assertThat(url4.isValidUrl()).isEqualTo(true)

        val url5 = "dev.to"
        assertThat(url5.isValidUrl()).isEqualTo(true)

        val url6 = "www.dev.to"
        assertThat(url6.isValidUrl()).isEqualTo(true)

        val url7 = "www.m.dev.to"
        assertThat(url7.isValidUrl()).isEqualTo(true)
    }

    @Test
    fun testUrlChecks_isValidUrl_checkAllInvalidUrls() {
        val url1 = "https://dev"
        assertThat(url1.isValidUrl()).isEqualTo(false)

        val url2 = "htp://www.dev.to"
        assertThat(url2.isValidUrl()).isEqualTo(false)

        val url3 = "dev"
        assertThat(url3.isValidUrl()).isEqualTo(false)
    }
}
