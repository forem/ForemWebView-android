package com.forem.android.data.remote.discover

import com.forem.android.data.remote.BaseDataSource
import com.forem.android.utility.Resource
import javax.inject.Inject

/** Injectable class to make API call for [DiscoverForemService]. */
class DiscoverForemRemoteDataSource @Inject constructor(
    private val discoverForemService: DiscoverForemService
) : BaseDataSource() {

    /** Makes API call using [DiscoverForemService] and returns [Resource]. */
    suspend fun getAllForems() = getResult { discoverForemService.getAllForems() }
}
