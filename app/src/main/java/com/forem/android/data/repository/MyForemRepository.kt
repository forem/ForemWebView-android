package com.forem.android.data.repository

import com.forem.android.app.model.Forem
import com.forem.android.data.local.CurrentForemLocalDataSource
import com.forem.android.data.local.ForemCollectionLocalDataSource
import com.forem.android.utility.performGetOperation
import javax.inject.Inject

/** Central class to connect UI with local database to get and save forems. */
class MyForemRepository @Inject constructor(
    private val localDataSource: ForemCollectionLocalDataSource,
    private val currentForemLocalDataSource: CurrentForemLocalDataSource
) {

    fun getMyForems() = performGetOperation(
        databaseQuery = { localDataSource.getAllForems }
    )

    suspend fun addToMyForems(forem: Forem) = localDataSource.insertForem(forem)

    suspend fun deleteForem(forem: Forem) = localDataSource.deleteForem(forem)

    fun getCurrentForem() = performGetOperation(
        databaseQuery = { currentForemLocalDataSource.getCurrentForem }
    )

    suspend fun updateCurrentForem(forem: Forem) =
        currentForemLocalDataSource.updateCurrentForem(forem)

    suspend fun removeCurrentForem() =
        currentForemLocalDataSource.removeCurrentForem()
}
