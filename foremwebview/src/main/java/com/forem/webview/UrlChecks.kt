package com.forem.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Patterns
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.Locale

/** Helper object which checks different urls with helper functions. */
object UrlChecks {
    private val overrideUrlList = listOf(
        "account.forem.com",
        "api.twitter.com/oauth",
        "api.twitter.com/login/error",
        "api.twitter.com/account/login_verification",
        "twitter.com",
        "github.com/login",
        "github.com/sessions/"
    )

    private val oauthUrlList = listOf(
        "https://accounts.google.com",
        "https://accounts.google.co.in"
    )

    /**
     * Function which determines the url type w.r.t. to host url.
     *
     * @param url which will get compared with the host.
     * @param host is the main url for comparison.
     * @return an emum described within from [UrlType]
     */
    @SuppressLint("DefaultLocale")
    fun getURLType(url: String, host: String): UrlType {
        return if (checkUrlIsCorrect(url)) {
            when {
                URL(url).host.lowercase(Locale.getDefault()).contains(host.lowercase(Locale.getDefault())) -> {
                    UrlType.HOST_LINK
                }
                overrideUrlList.any { url.lowercase(Locale.getDefault()).contains(it.lowercase(Locale.getDefault())) } -> {
                    UrlType.OVERRIDE_LINK
                }
                oauthUrlList.any { url.lowercase(Locale.getDefault()).startsWith(it.lowercase(Locale.getDefault())) } -> {
                    UrlType.OAUTH_LINK
                }
                else -> {
                    UrlType.THIRD_PARTY_LINK
                }
            }
        } else {
            if (url.lowercase(Locale.getDefault()).startsWith("mailto:")) {
                UrlType.EMAIL_LINK
            } else {
                UrlType.THIRD_PARTY_LINK
            }
        }
    }

    /**
     * Checks if text is correct valid url or not.
     * @param url which needs to be checked.
     * @throws MalformedURLException or URISyntaxException or Exception if url is not valid.
     * @return true if its a valid url.
     */
    fun checkUrlIsCorrect(url: String): Boolean {
        return try {
            URL(url)
            Uri.parse(url)
            return true
        } catch (e: MalformedURLException) {
            false
        } catch (e: URISyntaxException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the url is valid and then append/adjust to https:// prefix
     *
     * @param url which needs to be corrected.
     * @return a valid https url
     *
     * @sample: if url is "http://example.com" it will return "https://example.com"
     */
    fun checkAndAppendHttpsToUrl(url: String): String {
        return if (url.isValidUrl()) {
            when {
                url.contains("https://") -> {
                    url
                }
                url.contains("http://") -> {
                    url.replace("http://", "https://")
                }
                else -> {
                    "https://$url"
                }
            }
        } else {
            url
        }
    }

    /** Extension function to check if string is valid url. */
    fun String.isValidUrl(): Boolean = Patterns.WEB_URL.matcher(this).matches()
}
