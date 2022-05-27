package com.forem.android.data.remote.discover

import com.forem.android.BuildConfig
import com.forem.android.app.model.ForemCollection
import retrofit2.Response
import retrofit2.http.GET

/** Service that provides access to all public forems. */
interface DiscoverForemService {
    @GET(BuildConfig.FOREM_DISCOVER_URL)
    suspend fun getAllForems(): Response<ForemCollection>
}
