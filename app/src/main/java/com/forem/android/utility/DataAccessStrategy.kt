package com.forem.android.utility

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.forem.android.utility.Resource.Status.ERROR
import com.forem.android.utility.Resource.Status.SUCCESS
import kotlinx.coroutines.Dispatchers

/**
 *  Performs get operation on given database query and if the result is successful then it emits the
 *  data and continue with the network call to fetch latest data in background.
 *  If the result of database query is failure then network call will take place on success on which
 *  the saveCallResult function will be called to save the data locally.
 *
 *  Reference: https://github.com/sberoch/RickAndMorty-AndroidArchitectureSample/blob/master/app/src/main/java/com/example/rickandmorty/utils/DataAccessStrategy.kt
 */
fun <T, A> performGetOperation(
    databaseQuery: () -> LiveData<T>,
    networkCall: suspend () -> Resource<A>,
    saveNetworkCallResult: suspend (A) -> Unit
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val source: LiveData<Resource<T>> = databaseQuery.invoke().map { Resource.success(it) }
        emitSource(source)

        val responseStatus = networkCall.invoke()
        if (responseStatus.status == SUCCESS) {
            saveNetworkCallResult(responseStatus.data!!)
        } else if (responseStatus.status == ERROR) {
            emit(Resource.error(responseStatus.message!!))
            emitSource(source)
        }
    }

/**
 * Performs get operation on local database and returns the result. In this there is no network call
 * involved.
 */
fun <T> performGetOperation(
    databaseQuery: () -> LiveData<T>
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val source: LiveData<Resource<T>> = databaseQuery.invoke().map { Resource.success(it) }
        emitSource(source)
    }

/**
 * Performs get operation on network and returns the result. In this there is no local database
 * involvement.
 */
fun <T> performGetOperation(
    networkCall: suspend () -> Resource<T>
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val response = networkCall.invoke()
        if (response.status == SUCCESS) {
            emitSource(MutableLiveData(Resource.success(response.data!!)))
        } else if (response.status == ERROR) {
            emit(Resource.error(response.message!!))
        }
    }
