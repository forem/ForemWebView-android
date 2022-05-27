package com.forem.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Patterns
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

object UrlChecks {
    private val overrideUrlList = listOf(
        "account.forem.com",
        "api.twitter.com/oauth",
        "api.twitter.com/login/error",
        "api.twitter.com/account/login_verification",
        "github.com/login",
        "github.com/sessions/"
    )

    private val oauthUrlList = listOf(
        "https://accounts.google.com",
        "https://accounts.google.co.in"
    )

    @SuppressLint("DefaultLocale")
    fun getURLType(url: String, host: String): UrlType {
        // TODO(#178): Special case- if URL is is https://m.facebook.com/ and host is www.facebook.com
        //  the result is THIRD_PARTY_LINK but instead is should be HOST_LINK.
        return if (checkUrlIsCorrect(url)) {
            when {
                URL(url).host.toLowerCase().contains(host.toLowerCase()) -> {
                    UrlType.HOST_LINK
                }
                overrideUrlList.any { url.toLowerCase().contains(it.toLowerCase()) } -> {
                    UrlType.OVERRIDE_LINK
                }
                oauthUrlList.any { url.toLowerCase().startsWith(it.toLowerCase()) } -> {
                    UrlType.OAUTH_LINK
                }
                else -> {
                    UrlType.THIRD_PARTY_LINK
                }
            }
        } else {
            if (url.toLowerCase().startsWith("mailto:")) {
                UrlType.EMAIL_LINK
            } else {
                UrlType.THIRD_PARTY_LINK
            }
        }
    }

    fun checkUrlIsCorrect(url: String): Boolean {
        return try {
            val checkURL = URL(url)
            val checkURI = Uri.parse(url)
            return true
        } catch (e: MalformedURLException) {
            false
        } catch (e: URISyntaxException) {
            false
        } catch (e: Exception) {
            false
        }
    }

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
