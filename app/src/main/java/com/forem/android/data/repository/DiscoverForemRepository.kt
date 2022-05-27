package com.forem.android.data.repository

import androidx.lifecycle.LiveData
import com.forem.android.app.model.ForemCollection
import com.forem.android.data.local.ForemCollectionLocalDataSource
import com.forem.android.data.remote.discover.DiscoverForemRemoteDataSource
import com.forem.android.data.remote.discover.DiscoverForemService
import com.forem.android.utility.Resource
import com.forem.android.utility.performGetOperation
import javax.inject.Inject

/** Central class to connect UI with API call to get all forems list. */
class DiscoverForemRepository @Inject constructor(
    private val localDataSource: ForemCollectionLocalDataSource,
    private val remoteDataSource: DiscoverForemRemoteDataSource
) {

    /** Makes a network call to [DiscoverForemService] and returns data in [ForemCollection]. */
    fun getDiscoverForems(): LiveData<Resource<ForemCollection>> = performGetOperation(
        databaseQuery = { localDataSource.getAllDiscoverForems },
        networkCall = { remoteDataSource.getAllForems() },
        saveNetworkCallResult = { localDataSource.insertAllDiscoverForems(it) }
    )
}
