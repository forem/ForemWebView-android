package com.forem.android.data.local

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.forem.android.app.model.Forem
import com.forem.android.app.model.ForemCollection
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URL
import javax.inject.Inject

/** Injectable class to interact with local database for [ForemCollection]. */
class ForemCollectionLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val getAllForems: LiveData<ForemCollection> =
        context.myForemCollectionDataStore.data.asLiveData()

    val getAllDiscoverForems: LiveData<ForemCollection> =
        context.discoverForemCollectionDataStore.data.asLiveData()

    suspend fun insertForem(forem: Forem) {
        context.myForemCollectionDataStore.updateData {
            val homePageUrl = URL(forem.homePageUrl)
            it.toBuilder().putForem(homePageUrl.host, forem).build()
        }
    }

    suspend fun insertAllDiscoverForems(foremCollection: ForemCollection) {
        context.discoverForemCollectionDataStore.updateData {
            val builder = it.toBuilder()
            builder.clear()

            foremCollection.foremMap.forEach { (_, forem) ->
                val homePageUrl = URL(forem.homePageUrl)
                builder.putForem(homePageUrl.host, forem)
            }
            builder.build()
        }
    }

    suspend fun deleteForem(deleteForem: Forem) {
        context.myForemCollectionDataStore.updateData {
            val homePageUrl = URL(deleteForem.homePageUrl)
            it.toBuilder().removeForem(homePageUrl.host).build()
        }
    }
}
