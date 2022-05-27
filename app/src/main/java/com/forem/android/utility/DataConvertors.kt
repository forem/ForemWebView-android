package com.forem.android.utility

import com.forem.android.app.model.Forem
import java.net.URI

/** Central class to convert one form of data/forems into another form of data/forems. */
class DataConvertors {
    companion object {
        /** Accepts the url and converts that to a [Forem]. */
        fun createForemFromUrl(domain: String): Forem {
            val foremHomePageUrl = if (domain.contains("https:") || domain.contains("http")) {
                domain
            } else {
                "https://$domain"
            }
            val temporaryNameFromDomain = URI(domain).path
            return Forem.newBuilder()
                .setName(temporaryNameFromDomain)
                .setHomePageUrl(foremHomePageUrl.toLowerCase())
                .build()
        }
    }
}
