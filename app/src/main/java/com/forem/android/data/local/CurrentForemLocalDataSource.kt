package com.forem.android.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.forem.android.app.model.CurrentForem
import com.forem.android.app.model.Forem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Injectable class to interact with local database for [CurrentForem]. */
class CurrentForemLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val getCurrentForem: LiveData<CurrentForem> = context.currentForemDataStore.data.asLiveData()

    suspend fun updateCurrentForem(forem: Forem) {
        context.currentForemDataStore.updateData { currentForem ->
            currentForem.toBuilder().setForem(forem).build()
        }
    }

    suspend fun removeCurrentForem() {
        context.currentForemDataStore.updateData { currentForem ->
            currentForem.toBuilder().clearForem().build()
        }
    }
}
